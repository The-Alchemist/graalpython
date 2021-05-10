/* MIT License
 *  
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. 
 * Copyright (c) 2019 pyhandle
 *  
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *  
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


/*
   DO NOT EDIT THIS FILE!

   This file is automatically generated by hpy.tools.autogen.trampolines.autogen_trampolines_h
   See also hpy.tools.autogen and hpy/tools/public_api.h

   Run this to regenerate:
       make autogen

*/

#define UNWRAP(_h) ((_h)._i)
#define WRAP(_ptr) ((HPy){(_ptr)})
#define UNWRAP_TUPLE_BUILDER(_h) ((_h)._tup)
#define WRAP_TUPLE_BUILDER(_ptr) ((HPyTupleBuilder){(_ptr)})
#define UNWRAP_LIST_BUILDER(_h) ((_h)._lst)
#define WRAP_LIST_BUILDER(_ptr) ((HPyListBuilder){(_ptr)})
#define UNWRAP_TRACKER(_h) ((_h)._i)
#define WRAP_TRACKER(_ptr) ((HPyTracker){(_ptr)})

static inline HPy HPyModule_Create(HPyContext ctx, HPyModuleDef *def) {
     return WRAP(ctx->ctx_Module_Create ( ctx, def ));
}

static inline HPy HPy_Dup(HPyContext ctx, HPy h) {
     return WRAP(ctx->ctx_Dup ( ctx, UNWRAP(h) ));
}

static inline void HPy_Close(HPyContext ctx, HPy h) {
     ctx->ctx_Close ( ctx, UNWRAP(h) );
}

static inline HPy HPyLong_FromLong(HPyContext ctx, long value) {
     return WRAP(ctx->ctx_Long_FromLong ( ctx, value ));
}

static inline HPy HPyLong_FromUnsignedLong(HPyContext ctx, unsigned long value) {
     return WRAP(ctx->ctx_Long_FromUnsignedLong ( ctx, value )); 
}

static inline HPy HPyLong_FromLongLong(HPyContext ctx, long long v) {
     return WRAP(ctx->ctx_Long_FromLongLong ( ctx, v ));
}

static inline HPy HPyLong_FromUnsignedLongLong(HPyContext ctx, unsigned long long v) {
     return WRAP(ctx->ctx_Long_FromUnsignedLongLong ( ctx, v ));
}

static inline HPy HPyLong_FromSize_t(HPyContext ctx, size_t value) {
     return WRAP(ctx->ctx_Long_FromSize_t ( ctx, value )); 
}

static inline HPy HPyLong_FromSsize_t(HPyContext ctx, HPy_ssize_t value) {
     return WRAP(ctx->ctx_Long_FromSsize_t ( ctx, value )); 
}

static inline long HPyLong_AsLong(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsLong ( ctx, UNWRAP(h) );
}

static inline unsigned long HPyLong_AsUnsignedLong(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsUnsignedLong ( ctx, UNWRAP(h) ); 
}

static inline unsigned long HPyLong_AsUnsignedLongMask(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsUnsignedLongMask ( ctx, UNWRAP(h) ); 
}

static inline long long HPyLong_AsLongLong(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsLongLong ( ctx, UNWRAP(h) ); 
}

static inline unsigned long long HPyLong_AsUnsignedLongLong(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsUnsignedLongLong ( ctx, UNWRAP(h) ); 
}

static inline unsigned long long HPyLong_AsUnsignedLongLongMask(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsUnsignedLongLongMask ( ctx, UNWRAP(h) ); 
}

static inline size_t HPyLong_AsSize_t(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsSize_t ( ctx, UNWRAP(h) ); 
}

static inline HPy_ssize_t HPyLong_AsSsize_t(HPyContext ctx, HPy h) {
     return ctx->ctx_Long_AsSsize_t ( ctx, UNWRAP(h) ); 
}

static inline HPy HPyFloat_FromDouble(HPyContext ctx, double v) {
     return WRAP(ctx->ctx_Float_FromDouble ( ctx, v ));
}

static inline double HPyFloat_AsDouble(HPyContext ctx, HPy h) {
     return ctx->ctx_Float_AsDouble ( ctx, UNWRAP(h) ); 
}

static inline HPy HPyBool_FromLong(HPyContext ctx, long v) {
     return ctx->ctx_Bool_FromLong ( ctx, v ); 
}

static inline HPy_ssize_t HPy_Length(HPyContext ctx, HPy h) {
     return ctx->ctx_Length ( ctx, UNWRAP(h) ); 
}

static inline int HPyNumber_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_Number_Check ( ctx, UNWRAP(h) ); 
}

static inline HPy HPy_Add(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Add ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Subtract(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Subtract ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Multiply(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Multiply ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_MatrixMultiply(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_MatrixMultiply ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_FloorDivide(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_FloorDivide ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_TrueDivide(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_TrueDivide ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Remainder(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Remainder ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Divmod(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Divmod ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Power(HPyContext ctx, HPy h1, HPy h2, HPy h3) {
     return WRAP(ctx->ctx_Power ( ctx, UNWRAP(h1), UNWRAP(h2), UNWRAP(h3) )); 
}

static inline HPy HPy_Negative(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Negative ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_Positive(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Positive ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_Absolute(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Absolute ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_Invert(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Invert ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_Lshift(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Lshift ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Rshift(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Rshift ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_And(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_And ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Xor(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Xor ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Or(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_Or ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_Index(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Index ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_Long(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Long ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_Float(HPyContext ctx, HPy h1) {
     return WRAP(ctx->ctx_Float ( ctx, UNWRAP(h1) )); 
}

static inline HPy HPy_InPlaceAdd(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceAdd ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceSubtract(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceSubtract ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceMultiply(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceMultiply ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceMatrixMultiply(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceMatrixMultiply ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceFloorDivide(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceFloorDivide ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceTrueDivide(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceTrueDivide ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceRemainder(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceRemainder ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlacePower(HPyContext ctx, HPy h1, HPy h2, HPy h3) {
     return WRAP(ctx->ctx_InPlacePower ( ctx, UNWRAP(h1), UNWRAP(h2), UNWRAP(h3) )); 
}

static inline HPy HPy_InPlaceLshift(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceLshift ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceRshift(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceRshift ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceAnd(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceAnd ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceXor(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceXor ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline HPy HPy_InPlaceOr(HPyContext ctx, HPy h1, HPy h2) {
     return WRAP(ctx->ctx_InPlaceOr ( ctx, UNWRAP(h1), UNWRAP(h2) )); 
}

static inline int HPyCallable_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_Callable_Check ( ctx, UNWRAP(h) ); 
}

static inline HPy HPy_CallTupleDict(HPyContext ctx, HPy callable, HPy args, HPy kw) {
     return WRAP(ctx->ctx_CallTupleDict ( ctx, UNWRAP(callable), UNWRAP(args), UNWRAP(kw) )); 
}

static inline void HPyErr_SetString(HPyContext ctx, HPy h_type, const char *message) {
     ctx->ctx_Err_SetString ( ctx, UNWRAP(h_type), message );
}

static inline void HPyErr_SetObject(HPyContext ctx, HPy h_type, HPy h_value) {
     ctx->ctx_Err_SetObject ( ctx, UNWRAP(h_type), UNWRAP(h_value) ); 
}

static inline int HPyErr_Occurred(HPyContext ctx) {
     return ctx->ctx_Err_Occurred ( ctx );
}

static inline HPy HPyErr_NoMemory(HPyContext ctx) {
     return WRAP(ctx->ctx_Err_NoMemory ( ctx )); 
}

static inline void HPyErr_Clear(HPyContext ctx) {
     ctx->ctx_Err_Clear ( ctx ); 
}

static inline int HPy_IsTrue(HPyContext ctx, HPy h) {
     return ctx->ctx_IsTrue ( ctx, UNWRAP(h) ); 
}

static inline HPy HPyType_FromSpec(HPyContext ctx, HPyType_Spec *spec, HPyType_SpecParam *params) {
     return WRAP(ctx->ctx_Type_FromSpec ( ctx, spec, params )); 
}

static inline HPy HPyType_GenericNew(HPyContext ctx, HPy type, HPy *args, HPy_ssize_t nargs, HPy kw) {
     return WRAP(ctx->ctx_Type_GenericNew ( ctx, UNWRAP(type), args, nargs, UNWRAP(kw) ));
}

static inline HPy HPy_GetAttr(HPyContext ctx, HPy obj, HPy name) {
     return WRAP(ctx->ctx_GetAttr ( ctx, UNWRAP(obj), UNWRAP(name) ));
}

static inline HPy HPy_GetAttr_s(HPyContext ctx, HPy obj, const char *name) {
     return WRAP(ctx->ctx_GetAttr_s ( ctx, UNWRAP(obj), name ));
}

static inline int HPy_HasAttr(HPyContext ctx, HPy obj, HPy name) {
     return ctx->ctx_HasAttr ( ctx, UNWRAP(obj), UNWRAP(name) );
}

static inline int HPy_HasAttr_s(HPyContext ctx, HPy obj, const char *name) {
     return ctx->ctx_HasAttr_s ( ctx, UNWRAP(obj), name );
}

static inline int HPy_SetAttr(HPyContext ctx, HPy obj, HPy name, HPy value) {
     return ctx->ctx_SetAttr ( ctx, UNWRAP(obj), UNWRAP(name), UNWRAP(value));
}

static inline int HPy_SetAttr_s(HPyContext ctx, HPy obj, const char *name, HPy value) {
     return ctx->ctx_SetAttr_s ( ctx, UNWRAP(obj), name, UNWRAP(value));
}

static inline HPy HPy_GetItem(HPyContext ctx, HPy obj, HPy key) {
     return WRAP(ctx->ctx_GetItem ( ctx, UNWRAP(obj), UNWRAP(key)));
}

static inline HPy HPy_GetItem_i(HPyContext ctx, HPy obj, HPy_ssize_t idx) {
     return WRAP(ctx->ctx_GetItem_i ( ctx, UNWRAP(obj), idx ));
}

static inline HPy HPy_GetItem_s(HPyContext ctx, HPy obj, const char *key) {
     return WRAP(ctx->ctx_GetItem_s ( ctx, UNWRAP(obj), key ));
}

static inline int HPy_SetItem(HPyContext ctx, HPy obj, HPy key, HPy value) {
     return ctx->ctx_SetItem ( ctx, UNWRAP(obj), UNWRAP(key), UNWRAP(value));
}

static inline int HPy_SetItem_i(HPyContext ctx, HPy obj, HPy_ssize_t idx, HPy value) {
     return ctx->ctx_SetItem_i ( ctx, UNWRAP(obj), idx, UNWRAP(value));
}

static inline int HPy_SetItem_s(HPyContext ctx, HPy obj, const char *key, HPy value) {
     return ctx->ctx_SetItem_s ( ctx, UNWRAP(obj), key, UNWRAP(value));
}

static inline HPy HPy_Type(HPyContext ctx, HPy obj) {
     return ctx->ctx_Type ( ctx, obj ); 
}

static inline int HPy_TypeCheck(HPyContext ctx, HPy obj, HPy type) {
     return ctx->ctx_TypeCheck ( ctx, obj, type ); 
}

static inline void *_HPy_Cast(HPyContext ctx, HPy h) {
     return ctx->ctx_Cast ( ctx, UNWRAP(h) ); 
}

static inline HPy HPy_Repr(HPyContext ctx, HPy obj) {
     return WRAP(ctx->ctx_Repr ( ctx, UNWRAP(obj) )); 
}

static inline HPy HPy_Str(HPyContext ctx, HPy obj) {
     return WRAP(ctx->ctx_Str ( ctx, UNWRAP(obj) )); 
}

static inline HPy HPy_ASCII(HPyContext ctx, HPy obj) {
     return WRAP(ctx->ctx_ASCII ( ctx, UNWRAP(obj) )); 
}

static inline HPy HPy_Bytes(HPyContext ctx, HPy obj) {
     return WRAP(ctx->ctx_Bytes ( ctx, UNWRAP(obj) )); 
}

static inline HPy HPy_RichCompare(HPyContext ctx, HPy v, HPy w, int op) {
     return WRAP(ctx->ctx_RichCompare ( ctx, UNWRAP(v), UNWRAP(w), op )); 
}

static inline int HPy_RichCompareBool(HPyContext ctx, HPy v, HPy w, int op) {
     return ctx->ctx_RichCompareBool ( ctx, UNWRAP(v), UNWRAP(w), op );
}

static inline HPy_hash_t HPy_Hash(HPyContext ctx, HPy obj) {
     return ctx->ctx_Hash ( ctx, UNWRAP(obj) ); 
}

static inline int HPyBytes_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_Bytes_Check ( ctx, UNWRAP(h) );
}

static inline HPy_ssize_t HPyBytes_Size(HPyContext ctx, HPy h) {
     return ctx->ctx_Bytes_Size ( ctx, UNWRAP(h) );
}

static inline HPy_ssize_t HPyBytes_GET_SIZE(HPyContext ctx, HPy h) {
     return ctx->ctx_Bytes_GET_SIZE ( ctx, UNWRAP(h) );
}

static inline char *HPyBytes_AsString(HPyContext ctx, HPy h) {
     return ctx->ctx_Bytes_AsString ( ctx, UNWRAP(h) );
}

static inline char *HPyBytes_AS_STRING(HPyContext ctx, HPy h) {
     return ctx->ctx_Bytes_AS_STRING ( ctx, UNWRAP(h) );
}

static inline HPy HPyBytes_FromString(HPyContext ctx, const char *v) {
     return WRAP(ctx->ctx_Bytes_FromString ( ctx, v )); 
}

static inline HPy HPyBytes_FromStringAndSize(HPyContext ctx, const char *v, HPy_ssize_t len) {
     return WRAP(ctx->ctx_Bytes_FromStringAndSize ( ctx, v, len )); 
}

static inline HPy HPyUnicode_FromString(HPyContext ctx, const char *utf8) {
     return WRAP(ctx->ctx_Unicode_FromString ( ctx, utf8 ));
}

static inline int HPyUnicode_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_Unicode_Check ( ctx, UNWRAP(h) );
}

static inline HPy HPyUnicode_AsUTF8String(HPyContext ctx, HPy h) {
     return WRAP(ctx->ctx_Unicode_AsUTF8String ( ctx, UNWRAP(h) ));
}

static inline HPy HPyUnicode_FromWideChar(HPyContext ctx, const wchar_t *w, HPy_ssize_t size) {
     return WRAP(ctx->ctx_Unicode_FromWideChar ( ctx, w, size ));
}

static inline int HPyList_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_List_Check ( ctx, UNWRAP(h) ); 
}

static inline HPy HPyList_New(HPyContext ctx, HPy_ssize_t len) {
     return WRAP(ctx->ctx_List_New ( ctx, len ));
}

static inline int HPyList_Append(HPyContext ctx, HPy h_list, HPy h_item) {
     return ctx->ctx_List_Append ( ctx, UNWRAP(h_list), UNWRAP(h_item) );
}

static inline int HPyDict_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_Dict_Check ( ctx, UNWRAP(h) ); 
}

static inline HPy HPyDict_New(HPyContext ctx) {
     return WRAP(ctx->ctx_Dict_New ( ctx ));
}

static inline int HPyTuple_Check(HPyContext ctx, HPy h) {
     return ctx->ctx_Tuple_Check ( ctx, UNWRAP(h) ); 
}

static inline HPy HPyTuple_FromArray(HPyContext ctx, HPy items[], HPy_ssize_t n) {
     return WRAP(ctx->ctx_Tuple_FromArray ( ctx, (_HPyPtr)items, n ));
}

static inline HPy HPy_FromPyObject(HPyContext ctx, cpy_PyObject *obj) {
     return WRAP(ctx->ctx_FromPyObject ( ctx, obj )); 
}

static inline cpy_PyObject *HPy_AsPyObject(HPyContext ctx, HPy h) {
     return ctx->ctx_AsPyObject ( ctx, UNWRAP(h) ); 
}

static inline void _HPy_CallRealFunctionFromTrampoline(HPyContext ctx, HPyFunc_Signature sig, void *func, void *args) {
     ctx->ctx_CallRealFunctionFromTrampoline ( ctx, sig, func, args ); 
}

static inline void _HPy_CallDestroyAndThenDealloc(HPyContext ctx, void *func, cpy_PyObject *self) {
     ctx->ctx_CallDestroyAndThenDealloc ( ctx, func, self ); 
}

static inline HPyListBuilder HPyListBuilder_New(HPyContext ctx, HPy_ssize_t initial_size) {
     return WRAP_LIST_BUILDER(ctx->ctx_ListBuilder_New ( ctx, initial_size )); 
}

static inline void HPyListBuilder_Set(HPyContext ctx, HPyListBuilder builder, HPy_ssize_t index, HPy h_item) {
     ctx->ctx_ListBuilder_Set ( ctx, UNWRAP_LIST_BUILDER(builder), index, UNWRAP(h_item) ); 
}

static inline HPy HPyListBuilder_Build(HPyContext ctx, HPyListBuilder builder) {
     return WRAP(ctx->ctx_ListBuilder_Build ( ctx, UNWRAP_LIST_BUILDER(builder) )); 
}

static inline void HPyListBuilder_Cancel(HPyContext ctx, HPyListBuilder builder) {
     ctx->ctx_ListBuilder_Cancel ( ctx, UNWRAP_LIST_BUILDER(builder) ); 
}

static inline HPyTupleBuilder HPyTupleBuilder_New(HPyContext ctx, HPy_ssize_t initial_size) {
     return WRAP_TUPLE_BUILDER(ctx->ctx_TupleBuilder_New ( ctx, initial_size )); 
}

static inline void HPyTupleBuilder_Set(HPyContext ctx, HPyTupleBuilder builder, HPy_ssize_t index, HPy h_item) {
     ctx->ctx_TupleBuilder_Set ( ctx, UNWRAP_TUPLE_BUILDER(builder), index, UNWRAP(h_item) ); 
}

static inline HPy HPyTupleBuilder_Build(HPyContext ctx, HPyTupleBuilder builder) {
     return WRAP(ctx->ctx_TupleBuilder_Build ( ctx, UNWRAP_TUPLE_BUILDER(builder) )); 
}

static inline void HPyTupleBuilder_Cancel(HPyContext ctx, HPyTupleBuilder builder) {
     ctx->ctx_TupleBuilder_Cancel ( ctx, UNWRAP_TUPLE_BUILDER(builder));
}

static inline HPyTracker HPyTracker_New(HPyContext ctx, HPy_ssize_t size) {
     return WRAP_TRACKER(ctx->ctx_Tracker_New ( ctx, size )); 
}

static inline int HPyTracker_Add(HPyContext ctx, HPyTracker ht, HPy h) {
     return ctx->ctx_Tracker_Add ( ctx, UNWRAP_TRACKER(ht), UNWRAP(h) ); 
}

static inline void HPyTracker_ForgetAll(HPyContext ctx, HPyTracker ht) {
     ctx->ctx_Tracker_ForgetAll ( ctx, UNWRAP_TRACKER(ht) ); 
}

static inline void HPyTracker_Close(HPyContext ctx, HPyTracker ht) {
     ctx->ctx_Tracker_Close ( ctx, UNWRAP_TRACKER(ht) ); 
}

static inline void _HPy_Dump(HPyContext ctx, HPy h) {
     ctx->ctx_Dump ( ctx, UNWRAP(h) ); 
}

