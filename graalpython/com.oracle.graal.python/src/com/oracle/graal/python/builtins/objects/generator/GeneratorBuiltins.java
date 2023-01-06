/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates.
 * Copyright (c) 2014, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.builtins.objects.generator;

import static com.oracle.graal.python.builtins.PythonBuiltinClassType.AttributeError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.GeneratorExit;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.J___NAME__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.J___QUALNAME__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.T___NAME__;
import static com.oracle.graal.python.nodes.SpecialAttributeNames.T___QUALNAME__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___CLASS_GETITEM__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___ITER__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___NEXT__;
import static com.oracle.graal.python.nodes.SpecialMethodNames.J___REPR__;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.RuntimeError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.StopIteration;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.TypeError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.ValueError;

import java.util.List;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.modules.BuiltinFunctions;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.PythonAbstractObject;
import com.oracle.graal.python.builtins.objects.common.SequenceNodes.GetObjectArrayNode;
import com.oracle.graal.python.builtins.objects.exception.PBaseException;
import com.oracle.graal.python.builtins.objects.frame.PFrame;
import com.oracle.graal.python.builtins.objects.function.PArguments;
import com.oracle.graal.python.builtins.objects.str.StringNodes;
import com.oracle.graal.python.builtins.objects.str.StringUtils.SimpleTruffleStringFormatNode;
import com.oracle.graal.python.builtins.objects.traceback.GetTracebackNode;
import com.oracle.graal.python.builtins.objects.traceback.PTraceback;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.builtins.objects.type.TypeNodes;
import com.oracle.graal.python.nodes.ErrorMessages;
import com.oracle.graal.python.nodes.PGuards;
import com.oracle.graal.python.nodes.PRaiseNode;
import com.oracle.graal.python.nodes.SpecialMethodNames;
import com.oracle.graal.python.nodes.bytecode.FrameInfo;
import com.oracle.graal.python.nodes.bytecode.GeneratorReturnException;
import com.oracle.graal.python.nodes.bytecode.GeneratorYieldResult;
import com.oracle.graal.python.nodes.call.CallNode;
import com.oracle.graal.python.nodes.call.CallTargetInvokeNode;
import com.oracle.graal.python.nodes.call.GenericInvokeNode;
import com.oracle.graal.python.nodes.classes.IsSubtypeNode;
import com.oracle.graal.python.nodes.frame.MaterializeFrameNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonQuaternaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.object.IsBuiltinClassProfile;
import com.oracle.graal.python.runtime.PythonOptions;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.object.PythonObjectFactory;
import com.oracle.graal.python.util.PythonUtils;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;

@CoreFunctions(extendClasses = PythonBuiltinClassType.PGenerator)
public class GeneratorBuiltins extends PythonBuiltins {

    /**
     * Creates a fresh copy of the generator arguments to be used for the next invocation of the
     * generator. This is necessary to avoid persisting caller state. For example: If the generator
     * is invoked using {@code next(g)} outside of any {@code except} handler but the generator
     * requests the exception state, then the exception state will be written into the arguments. If
     * we now use the same arguments array every time, the next invocation would think that there is
     * not excepion but in fact, the a subsequent call ot {@code next} may have a different
     * exception state.
     *
     * <pre>
     *     g = my_generator()
     *
     *     # invoke without any exception context
     *     next(g)
     *
     *     try:
     *         raise ValueError
     *     except ValueError:
     *         # invoke with exception context
     *         next(g)
     * </pre>
     *
     * This is necessary for correct chaining of exceptions.
     */
    private static Object[] prepareArguments(PGenerator self) {
        Object[] generatorArguments = self.getArguments();
        Object[] arguments = new Object[generatorArguments.length];
        PythonUtils.arraycopy(generatorArguments, 0, arguments, 0, arguments.length);
        return arguments;
    }

    private static void checkResumable(PythonBuiltinBaseNode node, PGenerator self) {
        if (self.isFinished()) {
            throw node.raiseStopIteration();
        }
        if (self.isRunning()) {
            throw node.raise(ValueError, ErrorMessages.GENERATOR_ALREADY_EXECUTING);
        }
    }

    @ImportStatic({PGuards.class, PythonOptions.class})
    abstract static class ResumeGeneratorNode extends Node {
        public abstract Object execute(VirtualFrame frame, PGenerator self, Object sendValue);

        @Specialization(guards = "sameCallTarget(self.getCurrentCallTarget(), call.getCallTarget())", limit = "getCallSiteInlineCacheMaxDepth()")
        Object cached(VirtualFrame frame, PGenerator self, Object sendValue,
                        @Cached("createDirectCall(self.getCurrentCallTarget())") CallTargetInvokeNode call,
                        @Cached BranchProfile returnProfile,
                        @Cached IsBuiltinClassProfile errorProfile,
                        @Cached PRaiseNode raiseNode) {
            self.setRunning(true);
            Object[] arguments = prepareArguments(self);
            if (sendValue != null) {
                PArguments.setSpecialArgument(arguments, sendValue);
            }
            GeneratorYieldResult result;
            try {
                result = (GeneratorYieldResult) call.execute(frame, null, null, null, arguments);
            } catch (PException e) {
                throw handleException(self, errorProfile, raiseNode, e);
            } catch (GeneratorReturnException e) {
                returnProfile.enter();
                throw handleReturn(self, e, raiseNode);
            } finally {
                self.setRunning(false);
            }
            return handleResult(self, result);
        }

        @Specialization(replaces = "cached")
        @Megamorphic
        Object generic(VirtualFrame frame, PGenerator self, Object sendValue,
                        @Cached ConditionProfile hasFrameProfile,
                        @Cached GenericInvokeNode call,
                        @Cached BranchProfile returnProfile,
                        @Cached IsBuiltinClassProfile errorProfile,
                        @Cached PRaiseNode raiseNode) {
            self.setRunning(true);
            Object[] arguments = prepareArguments(self);
            if (sendValue != null) {
                PArguments.setSpecialArgument(arguments, sendValue);
            }
            GeneratorYieldResult result;
            try {
                if (hasFrameProfile.profile(frame != null)) {
                    result = (GeneratorYieldResult) call.execute(frame, self.getCurrentCallTarget(), arguments);
                } else {
                    result = (GeneratorYieldResult) call.execute(self.getCurrentCallTarget(), arguments);
                }
            } catch (PException e) {
                throw handleException(self, errorProfile, raiseNode, e);
            } catch (GeneratorReturnException e) {
                returnProfile.enter();
                throw handleReturn(self, e, raiseNode);
            } finally {
                self.setRunning(false);
            }
            return handleResult(self, result);
        }

        private PException handleException(PGenerator self, IsBuiltinClassProfile errorProfile, PRaiseNode raiseNode, PException e) {
            self.markAsFinished();
            // PEP 479 - StopIteration raised from generator body needs to be wrapped in
            // RuntimeError
            e.expectStopIteration(errorProfile);
            throw raiseNode.raise(RuntimeError, e.getEscapedException(), ErrorMessages.GENERATOR_RAISED_STOPITER);
        }

        private Object handleResult(PGenerator self, GeneratorYieldResult result) {
            self.handleResult(PythonLanguage.get(this), result);
            return result.yieldValue;
        }

        private static PException handleReturn(PGenerator self, GeneratorReturnException e, PRaiseNode raiseNode) {
            self.markAsFinished();
            if (e.value != PNone.NONE) {
                throw raiseNode.raise(StopIteration, new Object[]{e.value});
            } else {
                throw raiseNode.raise(StopIteration);
            }
        }

        protected static CallTargetInvokeNode createDirectCall(CallTarget target) {
            return CallTargetInvokeNode.create(target, false, true);
        }

        protected static boolean sameCallTarget(RootCallTarget target1, CallTarget target2) {
            return target1 == target2;
        }
    }

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return GeneratorBuiltinsFactory.getFactories();
    }

    @Builtin(name = J___NAME__, minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    abstract static class NameNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(noValue)")
        static Object getName(PGenerator self, @SuppressWarnings("unused") PNone noValue) {
            return self.getName();
        }

        @Specialization
        static Object setName(PGenerator self, TruffleString value) {
            self.setName(value);
            return PNone.NONE;
        }

        @Specialization(guards = "!isNoValue(value)")
        static Object setName(PGenerator self, Object value,
                        @Cached StringNodes.CastToTruffleStringCheckedNode cast) {
            return setName(self, cast.cast(value, ErrorMessages.MUST_BE_SET_TO_S_OBJ, T___NAME__, "string"));
        }
    }

    @Builtin(name = J___QUALNAME__, minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    abstract static class QualnameNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(noValue)")
        static Object getQualname(PGenerator self, @SuppressWarnings("unused") PNone noValue) {
            return self.getQualname();
        }

        @Specialization
        static Object setQualname(PGenerator self, TruffleString value) {
            self.setQualname(value);
            return PNone.NONE;
        }

        @Specialization(guards = "!isNoValue(value)")
        static Object setQualname(PGenerator self, Object value,
                        @Cached StringNodes.CastToTruffleStringCheckedNode cast) {
            return setQualname(self, cast.cast(value, ErrorMessages.MUST_BE_SET_TO_S_OBJ, T___QUALNAME__, "string"));
        }
    }

    @Builtin(name = J___ITER__, minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class IterNode extends PythonUnaryBuiltinNode {

        @Specialization
        static Object iter(PGenerator self) {
            return self;
        }
    }

    @Builtin(name = J___NEXT__, minNumOfPositionalArgs = 1, doc = "Implement next(self).")
    @GenerateNodeFactory
    public abstract static class NextNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object next(VirtualFrame frame, PGenerator self,
                        @Cached ResumeGeneratorNode resumeGeneratorNode) {
            checkResumable(this, self);
            return resumeGeneratorNode.execute(frame, self, null);
        }
    }

    @Builtin(name = "send", minNumOfPositionalArgs = 2)
    @GenerateNodeFactory
    public abstract static class SendNode extends PythonBinaryBuiltinNode {

        @Specialization
        Object send(VirtualFrame frame, PGenerator self, Object value,
                        @Cached ResumeGeneratorNode resumeGeneratorNode) {
            checkResumable(this, self);
            if (!self.isStarted() && value != PNone.NONE) {
                throw raise(TypeError, ErrorMessages.SEND_NON_NONE_TO_UNSTARTED_GENERATOR);
            }
            return resumeGeneratorNode.execute(frame, self, value);
        }
    }

    // throw(typ[,val[,tb]])
    @Builtin(name = "throw", minNumOfPositionalArgs = 2, maxNumOfPositionalArgs = 4)
    @GenerateNodeFactory
    public abstract static class ThrowNode extends PythonQuaternaryBuiltinNode {

        @Child private MaterializeFrameNode materializeFrameNode;
        @Child private GetTracebackNode getTracebackNode;

        @ImportStatic({PGuards.class, SpecialMethodNames.class})
        abstract static class PrepareExceptionNode extends Node {
            public abstract PBaseException execute(VirtualFrame frame, Object type, Object value);

            private PRaiseNode raiseNode;
            private IsSubtypeNode isSubtypeNode;

            @Specialization
            static PBaseException doException(PBaseException exc, @SuppressWarnings("unused") PNone value) {
                return exc;
            }

            @Specialization(guards = "!isPNone(value)")
            PBaseException doException(@SuppressWarnings("unused") PBaseException exc, @SuppressWarnings("unused") Object value) {
                throw raise().raise(PythonBuiltinClassType.TypeError, ErrorMessages.INSTANCE_EX_MAY_NOT_HAVE_SEP_VALUE);
            }

            @Specialization(guards = "isTypeNode.execute(type)", limit = "1")
            PBaseException doException(VirtualFrame frame, Object type, PBaseException value,
                            @SuppressWarnings("unused") @Shared("isType") @Cached TypeNodes.IsTypeNode isTypeNode,
                            @Cached BuiltinFunctions.IsInstanceNode isInstanceNode,
                            @Cached BranchProfile isNotInstanceProfile,
                            @Shared("callCtor") @Cached CallNode callConstructor) {
                if (isInstanceNode.executeWith(frame, value, type)) {
                    checkExceptionClass(type);
                    return value;
                } else {
                    isNotInstanceProfile.enter();
                    return doCreateObject(frame, type, value, isTypeNode, callConstructor);
                }
            }

            @Specialization(guards = "isTypeNode.execute(type)", limit = "1")
            PBaseException doCreate(VirtualFrame frame, Object type, @SuppressWarnings("unused") PNone value,
                            @SuppressWarnings("unused") @Shared("isType") @Cached TypeNodes.IsTypeNode isTypeNode,
                            @Shared("callCtor") @Cached CallNode callConstructor) {
                checkExceptionClass(type);
                Object instance = callConstructor.execute(frame, type);
                if (instance instanceof PBaseException) {
                    return (PBaseException) instance;
                } else {
                    return handleInstanceNotAnException(type, instance);
                }
            }

            @Specialization(guards = "isTypeNode.execute(type)", limit = "1")
            PBaseException doCreateTuple(VirtualFrame frame, Object type, PTuple value,
                            @SuppressWarnings("unused") @Shared("isType") @Cached TypeNodes.IsTypeNode isTypeNode,
                            @Cached GetObjectArrayNode getObjectArrayNode,
                            @Shared("callCtor") @Cached CallNode callConstructor) {
                checkExceptionClass(type);
                Object[] args = getObjectArrayNode.execute(value);
                Object instance = callConstructor.execute(frame, type, args);
                if (instance instanceof PBaseException) {
                    return (PBaseException) instance;
                } else {
                    return handleInstanceNotAnException(type, instance);
                }
            }

            @Specialization(guards = {"isTypeNode.execute(type)", "!isPNone(value)", "!isPTuple(value)", "!isPBaseException(value)"}, limit = "1")
            PBaseException doCreateObject(VirtualFrame frame, Object type, Object value,
                            @SuppressWarnings("unused") @Shared("isType") @Cached TypeNodes.IsTypeNode isTypeNode,
                            @Shared("callCtor") @Cached CallNode callConstructor) {
                checkExceptionClass(type);
                Object instance = callConstructor.execute(frame, type, value);
                if (instance instanceof PBaseException) {
                    return (PBaseException) instance;
                } else {
                    return handleInstanceNotAnException(type, instance);
                }
            }

            private static PBaseException handleInstanceNotAnException(Object type, Object instance) {
                /*
                 * Instead of throwing the exception here, we throw it into the generator. That's
                 * what CPython does
                 */
                return PythonObjectFactory.getUncached().createBaseException(TypeError, ErrorMessages.CALLING_N_SHOULD_HAVE_RETURNED_AN_INSTANCE_OF_BASE_EXCEPTION_NOT_P, new Object[]{type, instance});
            }

            @Fallback
            PBaseException doError(Object type, @SuppressWarnings("unused") Object value) {
                throw raise().raise(TypeError, ErrorMessages.EXCEPTIONS_MUST_BE_CLASSES_OR_INSTANCES_DERIVING_FROM_BASE_EX, type);
            }

            private PRaiseNode raise() {
                if (raiseNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    raiseNode = insert(PRaiseNode.create());
                }
                return raiseNode;
            }

            private void checkExceptionClass(Object type) {
                if (isSubtypeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    isSubtypeNode = insert(IsSubtypeNode.create());
                }
                if (!isSubtypeNode.execute(type, PythonBuiltinClassType.PBaseException)) {
                    throw raise().raise(TypeError, ErrorMessages.EXCEPTIONS_MUST_BE_CLASSES_OR_INSTANCES_DERIVING_FROM_BASE_EX, type);
                }
            }
        }

        @Specialization
        Object sendThrow(VirtualFrame frame, PGenerator self, Object typ, Object val, @SuppressWarnings("unused") PNone tb,
                        @Cached PrepareExceptionNode prepareExceptionNode,
                        @Cached ResumeGeneratorNode resumeGeneratorNode) {
            if (self.isRunning()) {
                throw raise(ValueError, ErrorMessages.GENERATOR_ALREADY_EXECUTING);
            }
            PBaseException instance = prepareExceptionNode.execute(frame, typ, val);
            return doThrow(frame, resumeGeneratorNode, self, instance, getLanguage());
        }

        @Specialization
        Object sendThrow(VirtualFrame frame, PGenerator self, Object typ, Object val, PTraceback tb,
                        @Cached PrepareExceptionNode prepareExceptionNode,
                        @Cached ResumeGeneratorNode resumeGeneratorNode) {
            if (self.isRunning()) {
                throw raise(ValueError, ErrorMessages.GENERATOR_ALREADY_EXECUTING);
            }
            PBaseException instance = prepareExceptionNode.execute(frame, typ, val);
            instance.setTraceback(tb);
            return doThrow(frame, resumeGeneratorNode, self, instance, getLanguage());
        }

        private Object doThrow(VirtualFrame frame, ResumeGeneratorNode resumeGeneratorNode, PGenerator self, PBaseException instance, PythonLanguage language) {
            instance.setContext(null); // Will be filled when caught
            if (self.isStarted() && !self.isFinished()) {
                instance.ensureReified();
                // Pass it to the generator where it will be thrown by the last yield, the location
                // will be filled there
                return resumeGeneratorNode.execute(frame, self, new ThrowData(instance, PythonOptions.isPExceptionWithJavaStacktrace(language)));
            } else {
                // Unstarted generator, we cannot pass the exception into the generator as there is
                // nothing that would handle it.
                // Instead, we throw the exception here and fake entering the generator by adding
                // its frame to the traceback manually.
                self.markAsFinished();
                Node location = self.getCurrentCallTarget().getRootNode();
                MaterializedFrame generatorFrame = PArguments.getGeneratorFrame(self.getArguments());
                PFrame pFrame = MaterializeFrameNode.materializeGeneratorFrame(location, generatorFrame, PFrame.Reference.EMPTY, factory());
                FrameInfo info = (FrameInfo) generatorFrame.getFrameDescriptor().getInfo();
                pFrame.setLine(info.getRootNode().getFirstLineno());
                PTraceback existingTraceback = null;
                if (instance.getTraceback() != null) {
                    existingTraceback = ensureGetTracebackNode().execute(instance.getTraceback());
                }
                PTraceback newTraceback = factory().createTraceback(pFrame, pFrame.getLine(), existingTraceback);
                instance.setTraceback(newTraceback);
                throw PException.fromObject(instance, location, PythonOptions.isPExceptionWithJavaStacktrace(language));
            }
        }

        @Specialization(guards = {"!isPNone(tb)", "!isPTraceback(tb)"})
        @SuppressWarnings("unused")
        Object doError(VirtualFrame frame, PGenerator self, Object typ, Object val, Object tb) {
            throw raise(TypeError, ErrorMessages.THROW_THIRD_ARG_MUST_BE_TRACEBACK);
        }

        private MaterializeFrameNode ensureMaterializeFrameNode() {
            if (materializeFrameNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                materializeFrameNode = insert(MaterializeFrameNode.create());
            }
            return materializeFrameNode;
        }

        private GetTracebackNode ensureGetTracebackNode() {
            if (getTracebackNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getTracebackNode = insert(GetTracebackNode.create());
            }
            return getTracebackNode;
        }
    }

    @Builtin(name = "close", minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    public abstract static class CloseNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object close(VirtualFrame frame, PGenerator self,
                        @Cached IsBuiltinClassProfile isGeneratorExit,
                        @Cached IsBuiltinClassProfile isStopIteration,
                        @Cached ResumeGeneratorNode resumeGeneratorNode,
                        @Cached ConditionProfile isStartedPorfile) {
            if (self.isRunning()) {
                throw raise(ValueError, ErrorMessages.GENERATOR_ALREADY_EXECUTING);
            }
            if (isStartedPorfile.profile(self.isStarted() && !self.isFinished())) {
                PBaseException pythonException = factory().createBaseException(GeneratorExit);
                // Pass it to the generator where it will be thrown by the last yield, the location
                // will be filled there
                boolean withJavaStacktrace = PythonOptions.isPExceptionWithJavaStacktrace(getLanguage());
                try {
                    resumeGeneratorNode.execute(frame, self, new ThrowData(pythonException, withJavaStacktrace));
                } catch (PException pe) {
                    if (isGeneratorExit.profileException(pe, GeneratorExit) || isStopIteration.profileException(pe, StopIteration)) {
                        // This is the "success" path
                        return PNone.NONE;
                    }
                    throw pe;
                } finally {
                    self.markAsFinished();
                }
                throw raise(RuntimeError, ErrorMessages.GENERATOR_IGNORED_EXIT);
            } else {
                self.markAsFinished();
                return PNone.NONE;
            }
        }
    }

    @Builtin(name = "gi_code", minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    public abstract static class GetCodeNode extends PythonUnaryBuiltinNode {
        @Specialization
        Object getCode(PGenerator self,
                        @Cached ConditionProfile hasCodeProfile) {
            return self.getOrCreateCode(hasCodeProfile, factory());
        }
    }

    @Builtin(name = "gi_running", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    public abstract static class GetRunningNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(none)")
        static Object getRunning(PGenerator self, @SuppressWarnings("unused") PNone none) {
            return self.isRunning();
        }

        @Specialization(guards = "!isNoValue(obj)")
        static Object setRunning(@SuppressWarnings("unused") PGenerator self, @SuppressWarnings("unused") Object obj,
                        @Cached PRaiseNode raiseNode) {
            throw raiseNode.raise(AttributeError, ErrorMessages.ATTRIBUTE_S_OF_P_OBJECTS_IS_NOT_WRITABLE, "gi_running", self);
        }
    }

    @Builtin(name = "gi_frame", minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    public abstract static class GetFrameNode extends PythonUnaryBuiltinNode {
        @Specialization
        static Object getFrame(PGenerator self,
                        @Cached PythonObjectFactory factory) {
            if (self.isFinished()) {
                return PNone.NONE;
            } else {
                MaterializedFrame generatorFrame = PArguments.getGeneratorFrame(self.getArguments());
                Node location = ((FrameInfo) generatorFrame.getFrameDescriptor().getInfo()).getRootNode();
                PFrame frame = MaterializeFrameNode.materializeGeneratorFrame(location, generatorFrame, PFrame.Reference.EMPTY, factory);
                FrameInfo info = (FrameInfo) generatorFrame.getFrameDescriptor().getInfo();
                int bci = self.getBci();
                frame.setLasti(bci);
                frame.setLine(info.getRootNode().bciToLine(bci));
                return frame;
            }
        }
    }

    @Builtin(name = "gi_yieldfrom", minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    public abstract static class GetYieldFromNode extends PythonUnaryBuiltinNode {
        @Specialization
        static Object getYieldFrom(PGenerator self) {
            Object yieldFrom = self.getYieldFrom();
            return yieldFrom != null ? yieldFrom : PNone.NONE;
        }
    }

    @Builtin(name = J___REPR__, minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class ReprNode extends PythonUnaryBuiltinNode {
        @Specialization
        TruffleString repr(PGenerator self,
                        @Cached SimpleTruffleStringFormatNode simpleTruffleStringFormatNode) {
            return simpleTruffleStringFormatNode.format("<generator object %s at %d>", self.getName(), PythonAbstractObject.objectHashCode(self));
        }
    }

    @Builtin(name = J___CLASS_GETITEM__, minNumOfPositionalArgs = 2, isClassmethod = true)
    @GenerateNodeFactory
    public abstract static class ClassGetItemNode extends PythonBinaryBuiltinNode {
        @Specialization
        Object classGetItem(Object cls, Object key) {
            return factory().createGenericAlias(cls, key);
        }
    }
}
