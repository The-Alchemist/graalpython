/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.objects.superobject;

import static com.oracle.graal.python.builtins.PythonBuiltinClassType.RuntimeError;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.J___SELF_CLASS__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.J___SELF__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.J___THISCLASS__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.T___CLASS__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___GETATTRIBUTE__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___GET__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___INIT__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___REPR__;
import static com.oracle.graal.python.nodes.truffle.TruffleStringMigrationHelpers.isJavaString;
import static com.oracle.graal.python.util.PythonUtils.TS_ENCODING;
import static com.oracle.graal.python.util.PythonUtils.toTruffleStringUncached;

import java.util.List;

import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.cell.CellBuiltins;
import com.oracle.graal.python.builtins.objects.cell.PCell;
import com.oracle.graal.python.builtins.objects.frame.PFrame;
import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.builtins.objects.object.ObjectBuiltins;
import com.oracle.graal.python.builtins.objects.object.ObjectBuiltinsFactory;
import com.oracle.graal.python.builtins.objects.str.PString;
import com.oracle.graal.python.builtins.objects.str.StringUtils.SimpleTruffleStringFormatNode;
import com.oracle.graal.python.builtins.objects.superobject.SuperBuiltinsFactory.GetObjectNodeGen;
import com.oracle.graal.python.builtins.objects.superobject.SuperBuiltinsFactory.GetObjectTypeNodeGen;
import com.oracle.graal.python.builtins.objects.superobject.SuperBuiltinsFactory.GetTypeNodeGen;
import com.oracle.graal.python.builtins.objects.superobject.SuperBuiltinsFactory.SuperInitNodeFactory;
import com.oracle.graal.python.builtins.objects.type.PythonAbstractClass;
import com.oracle.graal.python.builtins.objects.type.SpecialMethodSlot;
import com.oracle.graal.python.builtins.objects.type.TypeNodes;
import com.oracle.graal.python.builtins.objects.type.TypeNodes.GetMroNode;
import com.oracle.graal.python.builtins.objects.type.TypeNodes.IsSameTypeNode;
import com.oracle.graal.python.builtins.objects.type.TypeNodesFactory.IsSameTypeNodeGen;
import com.oracle.graal.python.nodes.ErrorMessages;
import com.oracle.graal.python.nodes.PNodeWithContext;
import com.oracle.graal.python.nodes.SpecialAttributeNames;
import com.oracle.graal.python.nodes.attributes.LookupInheritedSlotNode;
import com.oracle.graal.python.nodes.attributes.ReadAttributeFromObjectNode;
import com.oracle.graal.python.nodes.bytecode.FrameInfo;
import com.oracle.graal.python.nodes.bytecode.PBytecodeRootNode;
import com.oracle.graal.python.nodes.call.special.CallTernaryMethodNode;
import com.oracle.graal.python.nodes.call.special.LookupAndCallBinaryNode;
import com.oracle.graal.python.nodes.classes.IsSubtypeNode;
import com.oracle.graal.python.nodes.frame.ReadCallerFrameNode;
import com.oracle.graal.python.nodes.frame.ReadCallerFrameNode.FrameSelector;
import com.oracle.graal.python.nodes.function.BuiltinFunctionRootNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonTernaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonVarargsBuiltinNode;
import com.oracle.graal.python.nodes.object.GetClassNode;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.exception.PythonErrorType;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.TruffleString;

@CoreFunctions(extendClasses = PythonBuiltinClassType.Super)
public final class SuperBuiltins extends PythonBuiltins {
    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return SuperBuiltinsFactory.getFactories();
    }

    abstract static class GetTypeNode extends PNodeWithContext {

        abstract Object execute(SuperObject self);

        @Specialization(guards = {"isSingleContext()", "self == cachedSelf"}, assumptions = {"cachedSelf.getNeverReinitializedAssumption()"}, limit = "1")
        static Object cached(@NeverDefault @SuppressWarnings("unused") SuperObject self,
                        @SuppressWarnings("unused") @Cached("self") SuperObject cachedSelf,
                        @Cached(value = "self.getType()") Object type) {
            return type;
        }

        @Specialization(replaces = "cached")
        static Object uncached(SuperObject self) {
            return self.getType();
        }
    }

    abstract static class GetObjectTypeNode extends PNodeWithContext {

        abstract Object execute(SuperObject self);

        @Specialization(guards = {"isSingleContext()", "self == cachedSelf"}, assumptions = {"cachedSelf.getNeverReinitializedAssumption()"}, limit = "1")
        static Object cached(@NeverDefault @SuppressWarnings("unused") SuperObject self,
                        @SuppressWarnings("unused") @Cached("self") SuperObject cachedSelf,
                        @Cached(value = "self.getObjectType()") Object type) {
            return type;
        }

        @Specialization(replaces = "cached")
        static Object uncached(SuperObject self) {
            return self.getObjectType();
        }
    }

    abstract static class GetObjectNode extends PNodeWithContext {

        abstract Object execute(SuperObject self);

        @Specialization(guards = {"isSingleContext()", "self == cachedSelf"}, assumptions = {"cachedSelf.getNeverReinitializedAssumption()"}, limit = "1")
        static Object cached(@NeverDefault @SuppressWarnings("unused") SuperObject self,
                        @SuppressWarnings("unused") @Cached("self") SuperObject cachedSelf,
                        @Cached(value = "self.getObject()") Object object) {
            return object;
        }

        @Specialization(replaces = "cached")
        static Object uncached(SuperObject self) {
            return self.getObject();
        }
    }

    @Builtin(name = J___INIT__, minNumOfPositionalArgs = 1, takesVarArgs = true, takesVarKeywordArgs = true, alwaysNeedsCallerFrame = true)
    @GenerateNodeFactory
    public abstract static class SuperInitNode extends PythonVarargsBuiltinNode {
        @Child private IsSubtypeNode isSubtypeNode;
        @Child private GetClassNode getClassNode;
        @Child private LookupAndCallBinaryNode getAttrNode;
        @Child private CellBuiltins.GetRefNode getRefNode;
        @Child private TypeNodes.IsTypeNode isTypeNode;

        @Override
        public Object varArgExecute(VirtualFrame frame, @SuppressWarnings("unused") Object self, Object[] arguments, PKeyword[] keywords) throws VarargsBuiltinDirectInvocationNotSupported {
            if (keywords.length != 0) {
                throw raise(RuntimeError, ErrorMessages.UNEXPECTED_KEYWORD_ARGS, "super()");
            }
            if (arguments.length == 1) {
                return execute(frame, arguments[0], PNone.NO_VALUE, PNone.NO_VALUE);
            } else if (arguments.length == 2) {
                return execute(frame, arguments[0], arguments[1], PNone.NO_VALUE);
            } else if (arguments.length == 3) {
                return execute(frame, arguments[0], arguments[1], arguments[2]);
            } else {
                throw raise(RuntimeError, ErrorMessages.INVALID_NUMBER_OF_ARGUMENTS, "super()");
            }
        }

        @Override
        public final Object execute(VirtualFrame frame, Object self, Object[] arguments, PKeyword[] keywords) {
            if (keywords.length != 0) {
                throw raise(RuntimeError, ErrorMessages.UNEXPECTED_KEYWORD_ARGS, "super()");
            }
            if (arguments.length == 0) {
                return execute(frame, self, PNone.NO_VALUE, PNone.NO_VALUE);
            } else if (arguments.length == 1) {
                return execute(frame, self, arguments[0], PNone.NO_VALUE);
            } else if (arguments.length == 2) {
                return execute(frame, self, arguments[0], arguments[1]);
            } else {
                throw raise(RuntimeError, ErrorMessages.TOO_MANY_ARG, "super()");
            }
        }

        protected abstract Object execute(VirtualFrame frame, Object self, Object cls, Object obj);

        @Specialization(guards = "!isNoValue(cls)")
        PNone init(VirtualFrame frame, SuperObject self, Object cls, Object obj) {
            if (!(obj instanceof PNone)) {
                Object type = supercheck(frame, cls, obj);
                self.init(cls, type, obj);
            } else {
                self.init(cls, null, null);
            }
            return PNone.NONE;
        }

        protected boolean isInBuiltinFunctionRoot() {
            return getRootNode() instanceof BuiltinFunctionRootNode;
        }

        /**
         * Executed with the frame of the calling method - direct access to the frame.
         */
        @Specialization(guards = {"!isInBuiltinFunctionRoot()", "isNoValue(clsArg)", "isNoValue(objArg)"})
        PNone initInPlace(VirtualFrame frame, SuperObject self, @SuppressWarnings("unused") PNone clsArg, @SuppressWarnings("unused") PNone objArg) {
            PBytecodeRootNode rootNode = (PBytecodeRootNode) getRootNode();
            Frame localFrame = frame;
            if (rootNode.getCodeUnit().isGeneratorOrCoroutine()) {
                localFrame = PArguments.getGeneratorFrame(frame);
            }
            return initFromLocalFrame(frame, self, rootNode, localFrame);
        }

        /**
         * Executed within a {@link BuiltinFunctionRootNode} - indirect access to the frame.
         */
        @Specialization(guards = {"isInBuiltinFunctionRoot()", "isNoValue(clsArg)", "isNoValue(objArg)"})
        PNone init(VirtualFrame frame, SuperObject self, @SuppressWarnings("unused") PNone clsArg, @SuppressWarnings("unused") PNone objArg,
                        @Cached ReadCallerFrameNode readCaller) {
            PFrame target = readCaller.executeWith(frame, FrameSelector.SKIP_PYTHON_BUILTIN, 0);
            if (target == null) {
                throw raise(RuntimeError, ErrorMessages.NO_CURRENT_FRAME, "super()");
            }
            MaterializedFrame locals = target.getLocals();
            if (locals == null) {
                throw raise(RuntimeError, ErrorMessages.SUPER_NO_CLASS);
            }
            FrameInfo frameInfo = (FrameInfo) locals.getFrameDescriptor().getInfo();
            return initFromLocalFrame(frame, self, frameInfo.getRootNode(), locals);
        }

        private PNone initFromLocalFrame(VirtualFrame frame, SuperObject self, PBytecodeRootNode rootNode, Frame localFrame) {
            PCell classCell = rootNode.readClassCell(localFrame);
            if (classCell == null) {
                throw raise(RuntimeError, ErrorMessages.SUPER_NO_CLASS);
            }
            Object cls = getGetRefNode().execute(classCell);
            if (cls == null) {
                // the cell is empty
                throw raise(RuntimeError, ErrorMessages.SUPER_EMPTY_CLASS);
            }
            Object obj = rootNode.readSelf(localFrame);
            if (obj == null) {
                throw raise(RuntimeError, ErrorMessages.NO_ARGS, "super()");
            }
            return init(frame, self, cls, obj);
        }

        private CellBuiltins.GetRefNode getGetRefNode() {
            if (getRefNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getRefNode = insert(CellBuiltins.GetRefNode.create());
            }
            return getRefNode;
        }

        @SuppressWarnings("unused")
        @Fallback
        PNone initFallback(Object self, Object cls, Object obj) {
            throw raise(RuntimeError, ErrorMessages.INVALID_ARGS, "super()");
        }

        private IsSubtypeNode getIsSubtype() {
            if (isSubtypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isSubtypeNode = insert(IsSubtypeNode.create());
            }
            return isSubtypeNode;
        }

        private GetClassNode getGetClass() {
            if (getClassNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getClassNode = insert(GetClassNode.create());
            }
            return getClassNode;
        }

        private TypeNodes.IsTypeNode ensureIsTypeNode() {
            if (isTypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isTypeNode = insert(TypeNodes.IsTypeNode.create());
            }
            return isTypeNode;
        }

        private LookupAndCallBinaryNode getGetAttr() {
            if (getAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getAttrNode = insert(LookupAndCallBinaryNode.create(SpecialMethodSlot.GetAttribute));
            }
            return getAttrNode;
        }

        private Object supercheck(VirtualFrame frame, Object cls, Object object) {
            /*
             * Check that a super() call makes sense. Return a type object.
             *
             * obj can be a class, or an instance of one:
             *
             * - If it is a class, it must be a subclass of 'type'. This case is used for class
             * methods; the return value is obj.
             *
             * - If it is an instance, it must be an instance of 'type'. This is the normal case;
             * the return value is obj.__class__.
             *
             * But... when obj is an instance, we want to allow for the case where Py_TYPE(obj) is
             * not a subclass of type, but obj.__class__ is! This will allow using super() with a
             * proxy for obj.
             */
            if (ensureIsTypeNode().execute(object)) {
                if (getIsSubtype().execute(frame, object, cls)) {
                    return object;
                }
            }

            Object objectType = getGetClass().execute(object);
            if (getIsSubtype().execute(frame, objectType, cls)) {
                return objectType;
            } else {
                try {
                    Object classObject = getGetAttr().executeObject(frame, object, SpecialAttributeNames.T___CLASS__);
                    if (ensureIsTypeNode().execute(classObject)) {
                        if (getIsSubtype().execute(frame, classObject, cls)) {
                            return classObject;
                        }
                    }
                } catch (PException e) {
                    // error is ignored
                }

                throw raise(PythonErrorType.TypeError, ErrorMessages.SUPER_OBJ_MUST_BE_INST_SUB_OR_TYPE);
            }
        }
    }

    @Builtin(name = J___GET__, minNumOfPositionalArgs = 2, parameterNames = {"self", "obj", "type"})
    @GenerateNodeFactory
    public abstract static class GetNode extends PythonTernaryBuiltinNode {
        @Child GetObjectNode getObject = GetObjectNodeGen.create();
        @Child GetTypeNode getType;
        @Child SuperInitNode superInit;

        @Specialization
        public Object get(SuperObject self, Object obj, @SuppressWarnings("unused") Object type,
                        @Cached GetClassNode getClass) {
            if (obj == PNone.NONE || getObject.execute(self) != null) {
                // not binding to an object or already bound
                return this;
            } else {
                if (getType == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    superInit = insert(SuperInitNodeFactory.create());
                    getType = insert(GetTypeNodeGen.create());
                }
                SuperObject newSuper = factory().createSuperObject(getClass.execute(self));
                superInit.execute(null, newSuper, getType.execute(self), obj);
                return newSuper;
            }
        }
    }

    @Builtin(name = J___GETATTRIBUTE__, minNumOfPositionalArgs = 2)
    @GenerateNodeFactory
    public abstract static class GetattributeNode extends PythonBinaryBuiltinNode {
        @Child private ReadAttributeFromObjectNode readFromDict = ReadAttributeFromObjectNode.createForceType();
        @Child private LookupInheritedSlotNode readGet = LookupInheritedSlotNode.create(SpecialMethodSlot.Get);
        @Child private GetObjectTypeNode getObjectType = GetObjectTypeNodeGen.create();
        @Child private GetTypeNode getType;
        @Child private GetObjectNode getObject;
        @Child private CallTernaryMethodNode callGet;
        @Child private ObjectBuiltins.GetAttributeNode objectGetattributeNode;
        @Child private GetMroNode getMroNode;
        @Child private IsSameTypeNode isSameTypeNode;

        private Object genericGetAttr(VirtualFrame frame, Object object, Object attr) {
            if (objectGetattributeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                objectGetattributeNode = insert(ObjectBuiltinsFactory.GetAttributeNodeFactory.create());
            }
            return objectGetattributeNode.execute(frame, object, attr);
        }

        @Specialization
        public Object get(VirtualFrame frame, SuperObject self, Object attr,
                        @Cached TruffleString.EqualNode equalNode) {
            Object startType = getObjectType.execute(self);
            if (startType == null) {
                return genericGetAttr(frame, self, attr);
            }

            /*
             * We want __class__ to return the class of the super object (i.e. super, or a
             * subclass), not the class of su->obj.
             */
            TruffleString stringAttr = null;
            if (attr instanceof PString) {
                stringAttr = ((PString) attr).getValueUncached();
            } else if (isJavaString(attr)) {
                stringAttr = toTruffleStringUncached((String) attr);
            } else if (attr instanceof TruffleString) {
                stringAttr = (TruffleString) attr;
            }
            if (stringAttr != null) {
                if (equalNode.execute(stringAttr, T___CLASS__, TS_ENCODING)) {
                    return genericGetAttr(frame, self, T___CLASS__);
                }
            }

            // acts as a branch profile
            if (getType == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getType = insert(GetTypeNodeGen.create());
            }

            PythonAbstractClass[] mro = getMro(startType);
            /* No need to check the last one: it's gonna be skipped anyway. */
            int i = 0;
            int n = mro.length;
            for (i = 0; i + 1 < n; i++) {
                if (isSameType(getType.execute(self), mro[i])) {
                    break;
                }
            }
            i++; /* skip su->type (if any) */
            if (i >= n) {
                return genericGetAttr(frame, self, attr);
            }

            for (; i < n; i++) {
                PythonAbstractClass tmp = mro[i];
                Object res = readFromDict.execute(tmp, attr);
                if (res != PNone.NO_VALUE) {
                    Object get = readGet.execute(res);
                    if (get != PNone.NO_VALUE) {
                        /*
                         * Only pass 'obj' param if this is instance-mode super (See SF ID #743627)
                         */
                        // acts as a branch profile
                        if (callGet == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            getObject = insert(GetObjectNodeGen.create());
                            callGet = insert(CallTernaryMethodNode.create());
                        }
                        res = callGet.execute(frame, get, res, getObject.execute(self) == startType ? PNone.NONE : self.getObject(), startType);
                    }
                    return res;
                }
            }

            return genericGetAttr(frame, self, attr);
        }

        private boolean isSameType(Object execute, Object abstractPythonClass) {
            if (isSameTypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isSameTypeNode = insert(IsSameTypeNodeGen.create());
            }
            return isSameTypeNode.execute(execute, abstractPythonClass);
        }

        private PythonAbstractClass[] getMro(Object clazz) {
            if (getMroNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getMroNode = insert(GetMroNode.create());
            }
            return getMroNode.execute(clazz);
        }
    }

    @Builtin(name = J___THISCLASS__, minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    public abstract static class ThisClassNode extends PythonUnaryBuiltinNode {
        @Child GetTypeNode getType = GetTypeNodeGen.create();

        @Specialization
        Object getClass(SuperObject self) {
            Object type = getType.execute(self);
            if (type == null) {
                return PNone.NONE;
            }
            return type;
        }
    }

    @Builtin(name = J___SELF__, minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    public abstract static class SelfNode extends PythonUnaryBuiltinNode {
        @Child GetObjectNode getObject = GetObjectNodeGen.create();

        @Specialization
        Object getClass(SuperObject self) {
            Object object = getObject.execute(self);
            if (object == null) {
                return PNone.NONE;
            }
            return object;
        }
    }

    @Builtin(name = J___SELF_CLASS__, minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    public abstract static class SelfClassNode extends PythonUnaryBuiltinNode {
        @Child GetObjectTypeNode getObjectType = GetObjectTypeNodeGen.create();

        @Specialization
        Object getClass(SuperObject self) {
            Object objectType = getObjectType.execute(self);
            if (objectType == null) {
                return PNone.NONE;
            }
            return objectType;
        }
    }

    @Builtin(name = J___REPR__, minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class SuperReprNode extends PythonUnaryBuiltinNode {
        @Specialization
        TruffleString repr(SuperObject self,
                        @Cached TypeNodes.GetNameNode getNameNode,
                        @Cached GetTypeNode getType,
                        @Cached GetObjectTypeNode getObjectType,
                        @Cached SimpleTruffleStringFormatNode simpleTruffleStringFormatNode) {
            final Object type = getType.execute(self);
            final Object objType = getObjectType.execute(self);
            final Object typeName = type != null ? getNameNode.execute(type) : "NULL";
            if (objType != null) {
                return simpleTruffleStringFormatNode.format("<super: %s, <%s object>>", typeName, getNameNode.execute(objType));
            } else {
                return simpleTruffleStringFormatNode.format("<super: %s, NULL>", typeName);
            }
        }
    }
}
