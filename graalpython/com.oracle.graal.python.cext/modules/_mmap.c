/* Copyright (c) 2019, 2022, Oracle and/or its affiliates.
 * Copyright (C) 1996-2017 Python Software Foundation
 *
 * Licensed under the PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2
 */

#include "../src/capi.h"

#ifdef MS_WINDOWS
// we do not want to include windows sdk headers to build
typedef void* HANDLE;
#endif

typedef enum
{
    ACCESS_DEFAULT,
    ACCESS_READ,
    ACCESS_WRITE,
    ACCESS_COPY
} access_mode;

typedef struct mmap_object {
    PyObject_HEAD
    char *      data;
    Py_ssize_t  size;
    Py_ssize_t  pos;    /* relative to offset */
#ifdef MS_WINDOWS
    long long offset;
#else
    off_t       offset;
#endif
    Py_ssize_t  exports;

#ifdef MS_WINDOWS
    HANDLE      map_handle;
    HANDLE      file_handle;
    char *      tagname;
#endif

#ifdef UNIX
    int fd;
#endif

    PyObject *weakreflist;
    access_mode access;
} mmap_object;


POLYGLOT_DECLARE_TYPE(mmap_object);
static PyTypeObject mmap_object_type = PY_TRUFFLE_TYPE("mmap.mmap", NULL, Py_TPFLAGS_DEFAULT | Py_TPFLAGS_BASETYPE, sizeof(mmap_object));

int mmap_getbuffer(PyObject *self, Py_buffer *view, int flags) {
	// TODO(fa) readonly flag
    return PyBuffer_FillInfo(view, (PyObject*)self, mmap_object_data(self), PyObject_Size((PyObject *)self), 0, flags);
}

static PyObject* mmap_init_bufferprotocol(PyObject* self, PyObject* mmap_type) {
	assert(PyType_Check(mmap_type));

	initialize_type_structure(&mmap_object_type, (PyTypeObject*)mmap_type, polyglot_mmap_object_typeid());
	static PyBufferProcs mmap_as_buffer = {
	    (getbufferproc)mmap_getbuffer,
	    (releasebufferproc)NULL,
	};
	set_PyTypeObject_tp_as_buffer(&mmap_object_type, &mmap_as_buffer);

	return Py_None;
}

static struct PyMethodDef module_functions[] = {
    {"init_bufferprotocol", mmap_init_bufferprotocol, METH_O, NULL},
    {NULL,       NULL}          /* sentinel */
};

static struct PyModuleDef mmapmodule = {
    PyModuleDef_HEAD_INIT,
    "_mmap",
    NULL,
    -1,
    module_functions,
    NULL,
    NULL,
    NULL,
    NULL
};

PyMODINIT_FUNC
PyInit__mmap(void)
{
    PyObject *dict, *module;

    module = PyModule_Create(&mmapmodule);
    if (module == NULL)
        return NULL;
    dict = PyModule_GetDict(module);
    if (!dict)
        return NULL;
    PyDict_SetItemString(dict, "error", PyExc_OSError);

    return module;
}
