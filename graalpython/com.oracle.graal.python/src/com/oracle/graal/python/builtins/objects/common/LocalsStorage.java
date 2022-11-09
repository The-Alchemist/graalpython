/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.builtins.objects.common;

import static com.oracle.graal.python.nodes.frame.FrameSlotIDs.isUserFrameSlot;
import static com.oracle.graal.python.util.PythonUtils.TS_ENCODING;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.oracle.graal.python.builtins.objects.cell.PCell;
import com.oracle.graal.python.builtins.objects.common.HashingStorageLibrary.ForEachNode;
import com.oracle.graal.python.builtins.objects.common.HashingStorageNodes.SpecializedSetStringKey;
import com.oracle.graal.python.builtins.objects.str.PString;
import com.oracle.graal.python.lib.PyObjectHashNode;
import com.oracle.graal.python.lib.PyObjectRichCompareBool;
import com.oracle.graal.python.nodes.PGuards;
import com.oracle.graal.python.nodes.object.IsBuiltinClassProfile;
import com.oracle.graal.python.nodes.util.CastToTruffleStringNode;
import com.oracle.graal.python.util.PythonUtils;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;

@ExportLibrary(HashingStorageLibrary.class)
public final class LocalsStorage extends HashingStorage {
    /* This won't be the real (materialized) frame but a clone of it. */
    final MaterializedFrame frame;
    int len = -1;

    public LocalsStorage(FrameDescriptor fd) {
        this.frame = Truffle.getRuntime().createMaterializedFrame(PythonUtils.EMPTY_OBJECT_ARRAY, fd);
    }

    public LocalsStorage(MaterializedFrame frame) {
        this.frame = frame;
    }

    public MaterializedFrame getFrame() {
        return this.frame;
    }

    Object getValue(int slot) {
        return getValue(this.frame, slot);
    }

    static Object getValue(MaterializedFrame frame, int slot) {
        if (slot >= 0) {
            Object value = frame.getValue(slot);
            if (value instanceof PCell) {
                return ((PCell) value).getRef();
            }
            return value;
        }
        return null;
    }

    public int length() {
        if (this.len == -1) {
            CompilerDirectives.transferToInterpreter();
            calculateLength();
        }
        return this.len;
    }

    @TruffleBoundary
    void calculateLength() {
        FrameDescriptor fd = this.frame.getFrameDescriptor();
        int size = fd.getNumberOfSlots();
        for (int slot = 0; slot < fd.getNumberOfSlots(); slot++) {
            Object identifier = fd.getSlotName(slot);
            if (!isUserFrameSlot(identifier) || getValue(slot) == null) {
                size--;
            }
        }
        this.len = size;
    }

    public int lengthHint() {
        return len != -1 ? len : frame.getFrameDescriptor().getNumberOfSlots();
    }

    @ImportStatic(PGuards.class)
    @GenerateUncached
    static abstract class GetItemNode extends Node {
        /**
         * For builtin strings the {@code keyHash} value is ignored and can be garbage. If the
         * {@code keyHash} is equal to {@code -1} it will be computed for non-string keys.
         */
        public abstract Object execute(Frame frame, LocalsStorage self, Object key, long hash);

        @Specialization(guards = {"key == cachedKey", "desc == self.frame.getFrameDescriptor()"}, limit = "3")
        static Object getItemCached(LocalsStorage self, TruffleString key, @SuppressWarnings("unused") long hash,
                        @SuppressWarnings("unused") @Cached("key") TruffleString cachedKey,
                        @SuppressWarnings("unused") @Cached("self.frame.getFrameDescriptor()") FrameDescriptor desc,
                        @Cached("findSlot(desc, key)") int slot) {
            return self.getValue(slot);
        }

        @Specialization(replaces = "getItemCached")
        static Object string(LocalsStorage self, TruffleString key, @SuppressWarnings("unused") long hash) {
            if (!isUserFrameSlot(key)) {
                return null;
            }
            int slot = findSlot(self.frame.getFrameDescriptor(), key);
            return self.getValue(slot);
        }

        @Specialization(guards = "isBuiltinString(key, profile)", limit = "1")
        static Object pstring(LocalsStorage self, PString key, @SuppressWarnings("unused") long hash,
                        @SuppressWarnings("unused") @Shared("builtinProfile") @Cached IsBuiltinClassProfile profile,
                        @Cached CastToTruffleStringNode castToStringNode) {
            return string(self, castToStringNode.execute(key), -1);
        }

        @Specialization(guards = "!isBuiltinString(key, profile)", limit = "1")
        static Object notString(Frame frame, LocalsStorage self, Object key, long hashIn,
                        @SuppressWarnings("unused") @Shared("builtinProfile") @Cached IsBuiltinClassProfile profile,
                        @Cached PyObjectRichCompareBool.EqNode eqNode,
                        @Cached PyObjectHashNode hashNode) {
            long hash = hashIn == -1 ? hashNode.execute(frame, key) : hashIn;
            FrameDescriptor descriptor = self.frame.getFrameDescriptor();
            for (int slot = 0; slot < descriptor.getNumberOfSlots(); slot++) {
                Object currentKey = descriptor.getSlotName(slot);
                if (currentKey instanceof TruffleString) {
                    long keyHash = hashNode.execute(frame, currentKey);
                    if (keyHash == hash && eqNode.execute(frame, key, currentKey)) {
                        return self.getValue(slot);
                    }
                }
            }
            return null;
        }

        @TruffleBoundary
        static int findSlot(FrameDescriptor descriptor, TruffleString key) {
            for (int slot = 0; slot < descriptor.getNumberOfSlots(); slot++) {
                Object slotName = descriptor.getSlotName(slot);
                if (slotName instanceof TruffleString && key.equalsUncached((TruffleString) slotName, TS_ENCODING)) {
                    return slot;
                }
            }
            return -1;
        }
    }

    void addAllTo(HashingStorage storage, SpecializedSetStringKey putNode) {
        FrameDescriptor fd = this.frame.getFrameDescriptor();
        for (int slot = 0; slot < fd.getNumberOfSlots(); slot++) {
            Object identifier = fd.getSlotName(slot);
            if (isUserFrameSlot(identifier)) {
                Object value = getValue(slot);
                if (value != null) {
                    putNode.execute(storage, (TruffleString) identifier, value);
                }
            }
        }
    }

    @ExportMessage
    @TruffleBoundary
    @Override
    public Object forEachUntyped(ForEachNode<Object> node, Object arg) {
        Object result = arg;
        FrameDescriptor fd = this.frame.getFrameDescriptor();
        for (int slot = 0; slot < fd.getNumberOfSlots(); slot++) {
            Object identifier = fd.getSlotName(slot);
            if (isUserFrameSlot(identifier)) {
                Object value = getValue(slot);
                if (value != null) {
                    result = node.execute(identifier, result);
                }
            }
        }
        return result;
    }

    public HashingStorage copy() {
        return new LocalsStorage(this.frame);
    }

    protected abstract static class AbstractLocalsIterator implements Iterator<Object> {
        protected final MaterializedFrame frame;
        protected final int size;
        protected int index;

        AbstractLocalsIterator(MaterializedFrame frame) {
            this.frame = frame;
            this.size = frame.getFrameDescriptor().getNumberOfSlots();
        }

        public final int getState() {
            return index;
        }

        public final void setState(int state) {
            index = state;
        }

        @Override
        @TruffleBoundary
        public final Object next() {
            if (hasNext()) {
                int slot = index;
                loadNext();
                return frame.getFrameDescriptor().getSlotName(slot);
            }
            throw new NoSuchElementException();
        }

        @TruffleBoundary
        protected final boolean loadNext() {
            while (nextIndex()) {
                Object identifier = frame.getFrameDescriptor().getSlotName(index);
                if (isUserFrameSlot(identifier)) {
                    Object nextValue = getValue(this.frame, index);
                    if (nextValue != null) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected abstract boolean nextIndex();
    }

    private static final class LocalsIterator extends AbstractLocalsIterator {

        LocalsIterator(MaterializedFrame frame) {
            super(frame);
            index = -1;
        }

        @Override
        public boolean hasNext() {
            if (index == -1) {
                return loadNext();
            }
            return index < size;
        }

        @Override
        protected boolean nextIndex() {
            index++;
            return index < size;
        }

    }

    private static final class ReverseLocalsIterator extends AbstractLocalsIterator {

        ReverseLocalsIterator(MaterializedFrame frame) {
            super(frame);
            index = size;
        }

        @Override
        public boolean hasNext() {
            if (index == size) {
                return loadNext();
            }
            return index >= 0;
        }

        @Override
        protected boolean nextIndex() {
            index--;
            return index >= 0;
        }

    }
}
