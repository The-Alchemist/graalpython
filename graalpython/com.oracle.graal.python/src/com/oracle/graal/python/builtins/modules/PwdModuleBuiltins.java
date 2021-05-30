/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.python.builtins.modules;

import java.util.List;

import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.objects.tuple.StructSequence;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.truffle.PythonArithmeticTypes;
import com.oracle.graal.python.builtins.Python3Core;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.sun.security.auth.module.UnixSystem;

@CoreFunctions(defineModule = "pwd")
public class PwdModuleBuiltins extends PythonBuiltins {

    static final StructSequence.Descriptor STRUCT_PASSWD_DESC = new StructSequence.Descriptor(
                    PythonBuiltinClassType.PStructPasswd,
                    // @formatter:off The formatter joins these lines making it less readable
                    "pwd.struct_passwd: Results from getpw*() routines.\n\n" +
                    "This object may be accessed either as a tuple of\n" +
                    "  (pw_name,pw_passwd,pw_uid,pw_gid,pw_gecos,pw_dir,pw_shell)\n" +
                    "or via the object attributes as named in the above tuple.",
                    // @formatter:on
                    7,
                    new String[]{
                                    "pw_name", "pw_passwd", "pw_uid", "pw_gid", "pw_gecos", "pw_dir", "pw_shell",
                    },
                    new String[]{
                                    "user name", "password", "user id", "group id", "real name", "home directory", "shell program"
                    });

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return PwdModuleBuiltinsFactory.getFactories();
    }

    @Override
    public void initialize(Python3Core core) {
        super.initialize(core);
        StructSequence.initType(core, STRUCT_PASSWD_DESC);
    }

    @Builtin(name = "getpwuid", minNumOfPositionalArgs = 1, parameterNames = {"uid"})
    @GenerateNodeFactory
    @TypeSystemReference(PythonArithmeticTypes.class)
    public abstract static class GetpwuidNode extends PythonUnaryBuiltinNode {
        // TODO: this ignores the uid and just returns the info for the current user
        // TODO: this should use argument clinic to properly cast the argument
        // GR-28184

        @Specialization
        Object doGetpwuid(int uid) {
            return factory().createStructSeq(STRUCT_PASSWD_DESC, createPwuidObject(uid));
        }

        @Specialization
        Object doGetpwuid(long uid) {
            return factory().createStructSeq(STRUCT_PASSWD_DESC, createPwuidObject(uid));
        }

        @TruffleBoundary
        public Object[] createPwuidObject(Object uid) {
            String osName = System.getProperty("os.name");
            String username = System.getProperty("user.name");
            String password = "NOT_AVAILABLE";
            long gid = 0;
            String gecos = "";
            String homeDir = System.getProperty("user.home");
            String shell = "";
            if (osName.contains("Windows")) {
                // we keep base configs for now, could be changed in future, not tested on windows
            } else if (osName.contains("Linux")) {
                UnixSystem unix = new UnixSystem();
                gid = unix.getGid();
                shell = "/bin/sh";
            }

            return new Object[]{
                            username,
                            password,
                            uid,
                            gid,
                            gecos,
                            homeDir,
                            shell
            };
        }
    }

}
