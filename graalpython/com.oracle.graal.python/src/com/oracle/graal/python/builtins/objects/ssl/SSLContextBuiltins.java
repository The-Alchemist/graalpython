package com.oracle.graal.python.builtins.objects.ssl;

import static com.oracle.graal.python.builtins.PythonBuiltinClassType.SSLError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.TypeError;
import static com.oracle.graal.python.builtins.PythonBuiltinClassType.ValueError;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.oracle.graal.python.annotations.ArgumentClinic;
import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltinClassType;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.modules.SSLModuleBuiltins;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.bytes.PBytes;
import com.oracle.graal.python.builtins.objects.exception.OSErrorEnum;
import com.oracle.graal.python.builtins.objects.function.PKeyword;
import com.oracle.graal.python.builtins.objects.list.PList;
import com.oracle.graal.python.builtins.objects.object.PythonObject;
import com.oracle.graal.python.builtins.objects.object.PythonObjectLibrary;
import com.oracle.graal.python.builtins.objects.socket.PSocket;
import com.oracle.graal.python.nodes.ErrorMessages;
import com.oracle.graal.python.nodes.function.PythonBuiltinBaseNode;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonBinaryClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonQuaternaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonQuaternaryClinicBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.PythonUnaryBuiltinNode;
import com.oracle.graal.python.nodes.function.builtins.clinic.ArgumentClinicProvider;
import com.oracle.graal.python.nodes.truffle.PythonArithmeticTypes;
import com.oracle.graal.python.nodes.util.CannotCastException;
import com.oracle.graal.python.nodes.util.CastToJavaLongExactNode;
import com.oracle.graal.python.util.PythonUtils;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import javax.crypto.spec.DHParameterSpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.xml.bind.DatatypeConverter;

@CoreFunctions(extendClasses = PythonBuiltinClassType.PSSLContext)
public class SSLContextBuiltins extends PythonBuiltins {

    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String BEGIN_DH_PARAMETERS = "BEGIN DH PARAMETERS";
    private static final String END_DH_PARAMETERS = "END DH PARAMETERS";

    private static final String BEGIN_DH_PARAMETERS = "-----BEGIN DH PARAMETERS-----";
    private static final String END_DH_PARAMETERS = "-----END DH PARAMETERS-----";

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinBaseNode>> getNodeFactories() {
        return SSLContextBuiltinsFactory.getFactories();
    }

    @Builtin(name = "_SSLContext", constructsClass = PythonBuiltinClassType.PSSLContext, minNumOfPositionalArgs = 2, parameterNames = {"type", "protocol"})
    @ArgumentClinic(name = "protocol", conversion = ArgumentClinic.ClinicConversion.Int)
    @GenerateNodeFactory
    abstract static class SSLContextNode extends PythonBinaryClinicBuiltinNode {
        @Specialization
        PSSLContext createContext(Object type, int protocol) {
            SSLProtocolVersion version = SSLProtocolVersion.fromPythonId(protocol);
            if (version == null) {
                throw raise(ValueError, ErrorMessages.INVALID_OR_UNSUPPORTED_PROTOCOL_VERSION, "NULL");
            }
            try {
                boolean checkHostname;
                int verifyMode;
                if (version == SSLProtocolVersion.TLS_CLIENT) {
                    checkHostname = true;
                    verifyMode = SSLModuleBuiltins.SSL_CERT_REQUIRED;
                } else {
                    checkHostname = false;
                    verifyMode = SSLModuleBuiltins.SSL_CERT_NONE;
                }
                return factory().createSSLContext(type, version, SSLModuleBuiltins.X509_V_FLAG_TRUSTED_FIRST, checkHostname, verifyMode, createSSLContext(version));
            } catch (NoSuchAlgorithmException e) {
                throw raise(ValueError, ErrorMessages.INVALID_OR_UNSUPPORTED_PROTOCOL_VERSION, e.getMessage());
            } catch (KeyManagementException e) {
                // TODO when does this happen?
                throw raise(SSLError, e);
            }
        }

        @TruffleBoundary
        private static SSLContext createSSLContext(SSLProtocolVersion version) throws NoSuchAlgorithmException, KeyManagementException {
            SSLContext context = SSLContext.getInstance(version.getJavaId());
            context.init(null, null, null);
            return context;
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SSLContextBuiltinsClinicProviders.SSLContextNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "check_hostname", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    abstract static class CheckHostnameNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(none)")
        static boolean getCheckHostname(PSSLContext self, @SuppressWarnings("unused") PNone none) {
            return self.getCheckHostname();
        }

        @Specialization(guards = "!isNoValue(value)", limit = "3")
        static Object setCheckHostname(PSSLContext self, Object value,
                        @CachedLibrary("value") PythonObjectLibrary lib) {
            boolean checkHostname = lib.isTrue(value);
            if (checkHostname && self.getVerifyMode() == SSLModuleBuiltins.SSL_CERT_NONE) {
                self.setVerifyMode(SSLModuleBuiltins.SSL_CERT_REQUIRED);
            }
            self.setCheckHostname(checkHostname);
            return PNone.NONE;
        }
    }

    @Builtin(name = "verify_flags", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    abstract static class VerifyFlagsNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(none)")
        static long getVerifyFlags(PSSLContext self, @SuppressWarnings("unused") PNone none) {
            return self.getVerifyFlags();
        }

        @Specialization(guards = "!isNoValue(flags)", limit = "3")
        Object setVerifyFlags(VirtualFrame frame, PSSLContext self, Object flags,
                        @CachedLibrary("flags") PythonObjectLibrary lib,
                        @Cached CastToJavaLongExactNode castToLong) {
            try {
                self.setVerifyFlags((int) castToLong.execute(lib.asIndexWithFrame(flags, frame)));
            } catch (CannotCastException cannotCastException) {
                throw raise(TypeError, ErrorMessages.ARG_D_MUST_BE_S_NOT_P, "", "int", flags);
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "protocol", minNumOfPositionalArgs = 1, isGetter = true)
    @GenerateNodeFactory
    abstract static class ProtocolNode extends PythonUnaryBuiltinNode {
        @Specialization
        static int getProtocol(PSSLContext self) {
            return self.getVersion().getPythonId();
        }
    }

    @Builtin(name = "options", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    abstract static class OptionsNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(none)")
        static long getOptions(PSSLContext self, @SuppressWarnings("unused") PNone none) {
            return self.getOptions();
        }

        @Specialization(guards = "!isNoValue(valueObj)", limit = "3")
        static Object setOption(VirtualFrame frame, PSSLContext self, Object valueObj,
                        @Cached CastToJavaLongExactNode cast,
                        @CachedLibrary("valueObj") PythonObjectLibrary lib) {
            long value = cast.execute(lib.asIndexWithFrame(valueObj, frame));
            // TODO validate the options
            // TODO use the options
            self.setOptions(value);
            return PNone.NONE;
        }
    }

    @Builtin(name = "verify_mode", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    abstract static class VerifyModeNode extends PythonBinaryBuiltinNode {
        @Specialization(guards = "isNoValue(value)")
        static int get(PSSLContext self, @SuppressWarnings("unused") PNone value) {
            return self.getVerifyMode();
        }

        @Specialization(guards = "!isNoValue(value)", limit = "3")
        Object set(VirtualFrame frame, PSSLContext self, Object value,
                        @CachedLibrary("value") PythonObjectLibrary lib,
                        @Cached CastToJavaLongExactNode castToLong) {
            int mode;
            try {
                mode = (int) castToLong.execute(lib.asIndexWithFrame(value, frame));
            } catch (CannotCastException cannotCastException) {
                throw raise(TypeError, ErrorMessages.INTEGER_REQUIRED_GOT, value);
            }
            if (mode == SSLModuleBuiltins.SSL_CERT_NONE && self.getCheckHostname()) {
                throw raise(ValueError, ErrorMessages.CANNOT_SET_VERIFY_MODE_TO_CERT_NONE);
            }
            switch (mode) {
                case SSLModuleBuiltins.SSL_CERT_NONE:
                case SSLModuleBuiltins.SSL_CERT_OPTIONAL:
                case SSLModuleBuiltins.SSL_CERT_REQUIRED:
                    self.setVerifyMode(mode);
                    return PNone.NONE;
                default:
                    throw raise(ValueError, ErrorMessages.INVALID_VALUE_FOR_VERIFY_MODE);
            }
        }
    }

    @Builtin(name = "get_ciphers", minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class GetCiphersNode extends PythonUnaryBuiltinNode {
        @Specialization
        PList getCiphers(PSSLContext self) {
            String[] suites = self.getContext().getSupportedSSLParameters().getCipherSuites();
            Object[] dicts = new Object[suites.length];
            for (int i = 0; i < suites.length; i++) {
                // TODO: what other fields
                dicts[i] = factory().createDict(new PKeyword[]{new PKeyword("name", suites[i])});
            }
            return factory().createList(dicts);
        }
    }

    @Builtin(name = "set_ciphers", minNumOfPositionalArgs = 2, parameterNames = {"$self", "cipherlist"})
    @ArgumentClinic(name = "cipherlist", conversion = ArgumentClinic.ClinicConversion.String)
    @GenerateNodeFactory
    abstract static class SetCiphersNode extends PythonClinicBuiltinNode {
        @Specialization
        static Object getCiphers(PSSLContext self, String cipherlist) {
            String[] ciphers = cipherlist.split(":");
            // TODO: the openssl format comming from python isn't what jdk expects to be set
            // self.getContext().getSupportedSSLParameters().setCipherSuites(ciphers);
            return PNone.NONE;
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SSLContextBuiltinsClinicProviders.SetCiphersNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "num_tickets", minNumOfPositionalArgs = 1, maxNumOfPositionalArgs = 2, isGetter = true, isSetter = true)
    @GenerateNodeFactory
    @TypeSystemReference(PythonArithmeticTypes.class)
    abstract static class NumTicketsNode extends PythonBuiltinNode {
        @Specialization(guards = "isNoValue(value)")
        static int get(PSSLContext self, @SuppressWarnings("unused") PNone value) {
            return self.getNumTickets();
        }

        @Specialization(guards = "!isNoValue(value)", limit = "1")
        Object set(VirtualFrame frame, PSSLContext self, Object value,
                        @CachedLibrary("value") PythonObjectLibrary lib,
                        @Cached CastToJavaLongExactNode castToLong) {
            int num;
            try {
                num = (int) castToLong.execute(lib.asIndexWithFrame(value, frame));
            } catch (CannotCastException cannotCastException) {
                throw raise(TypeError, ErrorMessages.INTEGER_REQUIRED_GOT, value);
            }
            if (num < 0) {
                throw raise(ValueError, ErrorMessages.MUST_BE_NON_NEGATIVE, "value");
            }
            if (self.getVersion() != SSLProtocolVersion.TLS_SERVER) {
                throw raise(ValueError, ErrorMessages.SSL_CTX_NOT_SERVER_CONTEXT);
            }
            self.setNumTickets(num);
            return PNone.NONE;
        }
    }

    @Builtin(name = "set_default_verify_paths", minNumOfPositionalArgs = 1)
    @GenerateNodeFactory
    abstract static class SetDefaultVerifyPathsNode extends PythonBuiltinNode {
        @Specialization
        static Object set(PSSLContext self) {
            self.setDefaultVerifyPaths();
            return PNone.NONE;
        }
    }

    @Builtin(name = "load_verify_locations", minNumOfPositionalArgs = 1, parameterNames = {"$self", "cafile", "capath", "cadata"})
    @GenerateNodeFactory
    @TypeSystemReference(PythonArithmeticTypes.class)
    abstract static class LoadVerifyLocationsNode extends PythonQuaternaryBuiltinNode {
        @Specialization
        Object load(PSSLContext self, Object cafile, Object capath, Object cadata,
                        @CachedLibrary(limit = "2") PythonObjectLibrary fileLib,
                        @CachedLibrary(limit = "2") PythonObjectLibrary pathLib) {
            if (cafile instanceof PNone && capath instanceof PNone && cadata instanceof PNone) {
                throw raise(TypeError, ErrorMessages.CA_FILE_PATH_DATA_CANNOT_BE_ALL_OMMITED);
            }
            File file = null;
            if (!(cafile instanceof PNone)) {
                // TODO TruffleFile
                file = new File(fileLib.asPath(cafile));
                if (!file.exists()) {
                    throw raise(TypeError, ErrorMessages.S_SHOULD_BE_A_VALID_FILESYSTEMPATH, "cafile");
                }
            }

            File path = null;
            if (!(capath instanceof PNone)) {
                // TODO TruffleFile
                path = new File(pathLib.asPath(capath));
                if (!path.exists()) {
                    throw raise(TypeError, ErrorMessages.S_SHOULD_BE_A_VALID_FILESYSTEMPATH, "capath");
                }
            }

            if (!(cadata instanceof PNone)) {
                // TODO handle cadata
            }

            // TODO handle capath
            if (file != null) {
                // https://www.openssl.org/docs/man1.1.1/man3/SSL_CTX_load_verify_locations.html
                try {
                    X509Certificate[] certs = getCertificates(file);
                    if (certs.length == 0) {
                        // TODO: append any additional info? original msg is e.g. "[SSL] PEM lib
                        // (_ssl.c:3991)"
                        throw raise(SSLError, ErrorMessages.SSL_PEM_LIB_S, "no certificate found in certfile");
                    }
                    // TODO cache keystore?
                    KeyStore keystore = KeyStore.getInstance("JKS");
                    keystore.load(null);

                    for (X509Certificate cert : certs) {
                        // TODO what to use for alias
                        String alias = file != null ? file.getAbsolutePath() : path.getAbsolutePath() + ":" + cert.getIssuerX500Principal().getName() + ":" + cert.getSerialNumber();
                        keystore.setCertificateEntry(alias, cert);
                    }
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                    kmf.init(keystore, PythonUtils.EMPTY_CHAR_ARRAY);
                    KeyManager[] km = kmf.getKeyManagers();
                    self.getContext().init(km, null, null);
                } catch (Exception ex) {
                    // TODO
                    throw raise(SSLError, ex);
                }
            }

            return PNone.NONE;
        }
    }

    @Builtin(name = "load_cert_chain", minNumOfPositionalArgs = 2, parameterNames = {"$self", "certfile", "keyfile", "password"})
    @ArgumentClinic(name = "certfile", conversion = ArgumentClinic.ClinicConversion.String)
    @ArgumentClinic(name = "keyfile", conversion = ArgumentClinic.ClinicConversion.String, defaultValue = "\"\"", useDefaultForNone = true)
    @ArgumentClinic(name = "password", conversion = ArgumentClinic.ClinicConversion.String, defaultValue = "\"\"", useDefaultForNone = true)
    @GenerateNodeFactory
    @TypeSystemReference(PythonArithmeticTypes.class)
    abstract static class LoadCertChainNode extends PythonQuaternaryClinicBuiltinNode {
        @Specialization
        Object load(VirtualFrame frame, PSSLContext self, String certfile, String keyfile, String password) {
            // TODO trufflefile ?
            File certPem = new File(certfile);
            if (!certPem.exists()) {
                throw raiseOSError(frame, OSErrorEnum.ENOENT);
            }
            File pkPem = "".equals(keyfile) ? certPem : new File(keyfile);

            try {
                // TODO: preliminary implementation - import and convert PEM format to java keystore
                X509Certificate[] certificates = getCertificates(certPem);
                if (certificates.length == 0) {
                    // TODO: append any additional info? original msg is e.g. "[SSL] PEM lib
                    // (_ssl.c:3991)"
                    throw raise(SSLError, ErrorMessages.SSL_PEM_LIB_S, "no certificate found in certfile");
                }
                KeyStore keystore = KeyStore.getInstance("JKS");
                keystore.load(null);
                PrivateKey pk = getPrivateKey(pkPem);
                if (pk == null) {
                    // TODO: append any additional info? original msg is e.g. "[SSL] PEM lib
                    // (_ssl.c:3991)"
                    throw raise(SSLError, ErrorMessages.SSL_PEM_LIB_S, "no private key found in keyfile/certfile");
                }
                keystore.setKeyEntry(pkPem.getName(), pk, "".equals(password) ? password.toCharArray() : PythonUtils.EMPTY_CHAR_ARRAY, certificates);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(keystore, password.toCharArray());
                KeyManager[] km = kmf.getKeyManagers();
                self.getContext().init(km, null, null);
            } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | InvalidKeySpecException | UnrecoverableKeyException | KeyManagementException ex) {
                throw raise(SSLError, ErrorMessages.SSL_PEM_LIB_S, ex.getMessage());
            } catch (IOException ex) {
                // TODO
                throw raise(SSLError, ex.getMessage());
            }
            return PNone.NONE;
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SSLContextBuiltinsClinicProviders.LoadCertChainNodeClinicProviderGen.INSTANCE;
        }
    }

    @TruffleBoundary
    private static X509Certificate[] getCertificates(File pem) throws IOException, CertificateException {
        Base64.Decoder decoder = Base64.getDecoder();
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> res = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(pem))) {
            boolean sawBegin = false;
            StringBuilder sb = new StringBuilder(2000);
            String line;
            while ((line = r.readLine()) != null) {
                if (sawBegin) {
                    if (line.contains(END_CERTIFICATE)) {
                        sawBegin = false;
                        byte[] der;
                        try {
                            der = decoder.decode(sb.toString());
                        } catch (IllegalArgumentException e) {
                            throw new IOException("invalid base64");
                        }
                        res.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der)));
                    } else {
                        sb.append(line);
                    }
                } else if (line.contains(BEGIN_CERTIFICATE)) {
                    sawBegin = true;
                    sb.setLength(0);
                }
            }
            if (sawBegin) {
                throw new IOException("bad end line");
            }
        }
        return res.toArray(new X509Certificate[res.size()]);
    }

    private static PrivateKey getPrivateKey(File pkPem) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (BufferedReader r = new BufferedReader(new FileReader(pkPem))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains(BEGIN_PRIVATE_KEY)) {
                    break;
                }
            }
            if (line == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            while ((line = r.readLine()) != null) {
                if (line.contains(END_PRIVATE_KEY)) {
                    break;
                }
                sb.append(line);
            }
            byte[] bytes = DatatypeConverter.parseBase64Binary(sb.toString());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            final KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(spec);
        }
    }

    @Builtin(name = "_wrap_socket", minNumOfPositionalArgs = 2, parameterNames = {"$self", "sock", "server_side", "server_hostname"}, keywordOnlyNames = {"owner", "session"})
    @ArgumentClinic(name = "server_side", conversion = ArgumentClinic.ClinicConversion.Boolean, defaultValue = "false")
    @ArgumentClinic(name = "server_hostname", conversion = ArgumentClinic.ClinicConversion.String, defaultValue = "null", useDefaultForNone = true)
    @GenerateNodeFactory
    abstract static class WrapSocketNode extends PythonClinicBuiltinNode {
        @Specialization(guards = "serverHostname == null")
        Object wrap(PSSLContext context, PSocket sock, boolean serverSide, @SuppressWarnings("unused") Object serverHostname, Object owner, Object session) {
            return wrap(context, sock, serverSide, null, owner, session);
        }

        @Specialization
        // TODO parameters
        Object wrap(PSSLContext context, PSocket sock, boolean serverSide, String serverHostname, Object owner, Object session) {
            // TODO hostname
            // TODO hostname encode as IDNA?
            // TODO hostname can be null
            return factory().createSSLSocket(PythonBuiltinClassType.PSSLSocket, context, sock, createSSLEngine(context, !serverSide));
        }

        @TruffleBoundary
        private static SSLEngine createSSLEngine(PSSLContext context, boolean clientMode) {
            SSLEngine engine = context.getContext().createSSLEngine();
            engine.setUseClientMode(clientMode);
            if (context.getCiphers() != null) {
                engine.setEnabledCipherSuites(context.getCiphers());
            }
            return engine;
        }

        @Override
        protected ArgumentClinicProvider getArgumentClinic() {
            return SSLContextBuiltinsClinicProviders.WrapSocketNodeClinicProviderGen.INSTANCE;
        }
    }

    @Builtin(name = "load_dh_params", minNumOfPositionalArgs = 2, parameterNames = {"$self", "filepath"})
    @GenerateNodeFactory
    abstract static class LoadDhParamsNode extends PythonBinaryBuiltinNode {
        @Specialization
        PNone load(VirtualFrame frame, PSSLContext self, String filepath,
                        @Cached PRaiseSSLErrorNode raiseSSL) {
            File file = new File(filepath);
            if (!file.exists()) {
                throw raiseOSError(frame, OSErrorEnum.ENOENT);
            }
            DHParameterSpec dh = null;
            try {
                dh = getDHParameters(this, file);
                if (dh != null) {
                    self.setDHParameters(dh);
                }
            } catch (IOException | NoSuchAlgorithmException | InvalidParameterSpecException ex) {
                // TODO
                throw raise(ValueError, "load_dh_params: " + ex.getMessage());
            }
            return PNone.NONE;
        }

        @Specialization(limit = "3")
        PNone load(VirtualFrame frame, PSSLContext self, PBytes filepath,
                        @CachedLibrary("filepath") PythonObjectLibrary lib,
                        @Cached PRaiseSSLErrorNode raiseSSL) {
            load(frame, self, lib.asPath(filepath), raiseSSL);

            return PNone.NONE;
        }

        @Specialization(limit = "1")
        PNone load(VirtualFrame frame, PSSLContext self, PythonObject filepath,
                        @CachedLibrary("filepath") PythonObjectLibrary lib,
                        @Cached PRaiseSSLErrorNode raiseSSL) {
            load(frame, self, lib.asPath(filepath), raiseSSL);

            return PNone.NONE;
        }

        @Specialization(guards = {"!isString(filepath)", "!isBytes(filepath)", "!isPythonObject(filepath)"})
        Object wrap(PSSLContext self, Object filepath) {
            throw raise(TypeError, ErrorMessages.EXPECTED_STR_BYTE_OSPATHLIKE_OBJ, filepath);
        }
    }

    @TruffleBoundary
    private static DHParameterSpec getDHParameters(Node node, File file) throws IOException, NoSuchAlgorithmException, InvalidParameterSpecException {
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            // TODO: test me!
            String line;
            boolean begin = false;
            StringBuilder sb = new StringBuilder();
            while ((line = r.readLine()) != null) {
                if (line.contains(BEGIN_DH_PARAMETERS)) {
                    begin = true;
                } else if (begin) {
                    if (line.contains(END_DH_PARAMETERS)) {
                        break;
                    }
                    sb.append(line.trim());
                }
            }
            if (!begin) {
                throw PRaiseSSLErrorNode.raiseUncached(node, SSLErrorCode.ERROR_NO_START_LINE, ErrorMessages.SSL_PEM_NO_START_LINE);
            }
            AlgorithmParameters ap = AlgorithmParameters.getInstance("DH");
            ap.init(Base64.getDecoder().decode(sb.toString()));
            return ap.getParameterSpec(DHParameterSpec.class);
        }
    }

}
