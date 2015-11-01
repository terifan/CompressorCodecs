package org.terifan.compression.cabac265;

import static org.terifan.compression.cabac265.Cabac.*;


public class context_model_table 
{
//	enum context_model_index {
//	  // SAO
//	//  CONTEXT_MODEL_SAO_MERGE_FLAG = 0,
//	//  CONTEXT_MODEL_SAO_TYPE_IDX   = CONTEXT_MODEL_SAO_MERGE_FLAG +1,
//	//
//	//  // CB-tree
//	//  CONTEXT_MODEL_SPLIT_CU_FLAG  = CONTEXT_MODEL_SAO_TYPE_IDX + 1,
//	//  CONTEXT_MODEL_CU_SKIP_FLAG   = CONTEXT_MODEL_SPLIT_CU_FLAG + 3,
//	//
//	//  // intra-prediction
//	//  CONTEXT_MODEL_PART_MODE      = CONTEXT_MODEL_CU_SKIP_FLAG + 3,
//	//  CONTEXT_MODEL_PREV_INTRA_LUMA_PRED_FLAG = CONTEXT_MODEL_PART_MODE + 4,
//	//  CONTEXT_MODEL_INTRA_CHROMA_PRED_MODE    = CONTEXT_MODEL_PREV_INTRA_LUMA_PRED_FLAG + 1,
//	//
//	//  // transform-tree
//	//  CONTEXT_MODEL_CBF_LUMA                  = CONTEXT_MODEL_INTRA_CHROMA_PRED_MODE + 1,
//	//  CONTEXT_MODEL_CBF_CHROMA                = CONTEXT_MODEL_CBF_LUMA + 2,
//	//  CONTEXT_MODEL_SPLIT_TRANSFORM_FLAG      = CONTEXT_MODEL_CBF_CHROMA + 4,
//	//  CONTEXT_MODEL_CU_CHROMA_QP_OFFSET_FLAG  = CONTEXT_MODEL_SPLIT_TRANSFORM_FLAG + 3,
//	//  CONTEXT_MODEL_CU_CHROMA_QP_OFFSET_IDX   = CONTEXT_MODEL_CU_CHROMA_QP_OFFSET_FLAG + 1,
//	//
//	//  // residual
//	//  CONTEXT_MODEL_LAST_SIGNIFICANT_COEFFICIENT_X_PREFIX = CONTEXT_MODEL_CU_CHROMA_QP_OFFSET_IDX + 1,
//	//  CONTEXT_MODEL_LAST_SIGNIFICANT_COEFFICIENT_Y_PREFIX = CONTEXT_MODEL_LAST_SIGNIFICANT_COEFFICIENT_X_PREFIX + 18,
//	//  CONTEXT_MODEL_CODED_SUB_BLOCK_FLAG          = CONTEXT_MODEL_LAST_SIGNIFICANT_COEFFICIENT_Y_PREFIX + 18,
//	//  CONTEXT_MODEL_SIGNIFICANT_COEFF_FLAG        = CONTEXT_MODEL_CODED_SUB_BLOCK_FLAG + 4,
//	//  CONTEXT_MODEL_COEFF_ABS_LEVEL_GREATER1_FLAG = CONTEXT_MODEL_SIGNIFICANT_COEFF_FLAG + 42+2,
//	//  CONTEXT_MODEL_COEFF_ABS_LEVEL_GREATER2_FLAG = CONTEXT_MODEL_COEFF_ABS_LEVEL_GREATER1_FLAG + 24,
//	//
//	//  CONTEXT_MODEL_CU_QP_DELTA_ABS        = CONTEXT_MODEL_COEFF_ABS_LEVEL_GREATER2_FLAG + 6,
//	//  CONTEXT_MODEL_TRANSFORM_SKIP_FLAG    = CONTEXT_MODEL_CU_QP_DELTA_ABS + 2,
//	//  CONTEXT_MODEL_RDPCM_FLAG             = CONTEXT_MODEL_TRANSFORM_SKIP_FLAG + 2,
//	//  CONTEXT_MODEL_RDPCM_DIR              = CONTEXT_MODEL_RDPCM_FLAG + 2,
//	//
//	//  // motion
//	//  CONTEXT_MODEL_MERGE_FLAG             = CONTEXT_MODEL_RDPCM_DIR + 2,
//	//  CONTEXT_MODEL_MERGE_IDX              = CONTEXT_MODEL_MERGE_FLAG + 1,
//	//  CONTEXT_MODEL_PRED_MODE_FLAG         = CONTEXT_MODEL_MERGE_IDX + 1,
//	//  CONTEXT_MODEL_ABS_MVD_GREATER01_FLAG = CONTEXT_MODEL_PRED_MODE_FLAG + 1,
//	//  CONTEXT_MODEL_MVP_LX_FLAG            = CONTEXT_MODEL_ABS_MVD_GREATER01_FLAG + 2,
//	//  CONTEXT_MODEL_RQT_ROOT_CBF           = CONTEXT_MODEL_MVP_LX_FLAG + 1,
//	//  CONTEXT_MODEL_REF_IDX_LX             = CONTEXT_MODEL_RQT_ROOT_CBF + 1,
//	//  CONTEXT_MODEL_INTER_PRED_IDC         = CONTEXT_MODEL_REF_IDX_LX + 2,
//	//  CONTEXT_MODEL_CU_TRANSQUANT_BYPASS_FLAG = CONTEXT_MODEL_INTER_PRED_IDC + 5,
//	//  CONTEXT_MODEL_LOG2_RES_SCALE_ABS_PLUS1 = CONTEXT_MODEL_CU_TRANSQUANT_BYPASS_FLAG + 1,
//	//  CONTEXT_MODEL_RES_SCALE_SIGN_FLAG      = CONTEXT_MODEL_LOG2_RES_SCALE_ABS_PLUS1 + 8,
//	//  CONTEXT_MODEL_TABLE_LENGTH           = CONTEXT_MODEL_RES_SCALE_SIGN_FLAG + 2
//	};
//
//static int CONTEXT_MODEL_TABLE_LENGTH = 1;
//
//  context_model_table copy() { context_model_table t=this; t.decouple(); return t; }
//
//  boolean empty() { return refcnt != null; }
//
////  context_model& operator[](int i) { return model[i]; }
//
////  context_model_table& operator=(const context_model_table&);
//
////  bool operator==(const context_model_table&) const;
//
//  context_model[] model; // [CONTEXT_MODEL_TABLE_LENGTH]
//  Integer refcnt;
//
//boolean D = false;
//
//
//	public context_model_table()
//	{
//	}
//
//
//context_model_table(context_model_table src)
//{
//  if (D) System.out.printf("%p c'tor = %p\n",this,src);
//
//  if (src.refcnt != null) {
//    src.refcnt++;
//  }
//
//  refcnt = src.refcnt;
//  model  = src.model;
//}
//
//
////context_model_table::~context_model_table()
////{
////  if (D) printf("%p destructor\n",this);
////
////  if (refcnt) {
////    (*refcnt)--;
////    if (*refcnt==0) {
////      if (D) printf("mfree %p\n",model);
////      delete[] model;
////      delete refcnt;
////    }
////  }
////}
//
//
//void init(int initType, int QPY)
//{
//  if (D) System.out.printf("%p init\n",this);
//
//  decouple_or_alloc_with_empty_data();
//
//  initialize_CABAC_models(model, initType, QPY);
//}
//
//
//void release()
//{
//  if (D) System.out.printf("%p release %p\n",this,refcnt);
//
//  if (refcnt != null) { return; }
//
//  // if (*refcnt == 1) { return; } <- keep memory for later, but does not work when we believe that we freed the memory and nulled all references
//
//  refcnt--;
////  if (refcnt==0) {
////    delete[] model;
////    delete refcnt;
////  }
//
//  model = null;
//  refcnt= null;
//}
//
//
//void decouple()
//{
//  if (D) System.out.printf("%p decouple (%p)\n",this,refcnt);
//
////  assert(refcnt); // not necessarily so, but we never use it on an unitialized object
//
//  if (refcnt > 1) {
//    refcnt--;
//
//	assert false;
////    context_model oldModel = model;
//
//    model = new context_model[CONTEXT_MODEL_TABLE_LENGTH];
//    refcnt= 1;
//
////    memcpy(model,oldModel,sizeof(context_model)*CONTEXT_MODEL_TABLE_LENGTH);
//  }
//}
//
//
//context_model_table transfer()
//{
//  context_model_table newtable = new context_model_table();
//  newtable.model = model;
//  newtable.refcnt= refcnt;
//
//  model =null;
//  refcnt=null;
//
//  return newtable;
//}
//
//
//context_model_table replace(context_model_table src)
//{
//  if (D) System.out.printf("%p assign = %p\n",this,src);
//
//  // assert(src.refcnt); // not necessarily so, but we never use it on an unitialized object
//
//  if (src.refcnt != null) {
//    release();
//    return this;
//  }
//
//  src.refcnt++;
//
//  release();
//
//  model = src.model;
//  refcnt= src.refcnt;
//
//  return this;
//}
//
//
//boolean equals(context_model_table b)
//{
//  if (b.model == model) return true;
//  if (b.model == null || model == null) return false;
//
//  for (int i=0;i<CONTEXT_MODEL_TABLE_LENGTH;i++) {
//    if (!(b.model[i] == model[i])) return false;
//  }
//
//  return true;
//}
//
//
//String debug_dump()
//{
//  int hash = 0;
//  for (int i=0;i<CONTEXT_MODEL_TABLE_LENGTH;i++) {
//    hash ^= ((i+7)*model[i].state) & 0xFFFF;
//  }
//
//  return ""+hash;
//}
//
//
//void decouple_or_alloc_with_empty_data()
//{
//  if (refcnt!=null && refcnt==1) { return; }
//
//  if (refcnt!=null) {
//    refcnt--;
//  }
//
//  if (D) System.out.printf("%p (alloc)\n",this);
//
//  model = new context_model[CONTEXT_MODEL_TABLE_LENGTH];
//  refcnt= 1;
//}
//
//
//static void set_initValue(int SliceQPY, context_model[] model, int initValue, int nContexts)
//{
//  int slopeIdx = initValue >> 4;
//  int intersecIdx = initValue & 0xF;
//  int m = slopeIdx*5 - 45;
//  int n = (intersecIdx<<3) - 16;
//  int preCtxState = Clip3(1,126, ((m*Clip3(0,51, SliceQPY))>>4)+n);
//
//  // logtrace(LogSlice,"QP=%d slopeIdx=%d intersecIdx=%d m=%d n=%d\n",SliceQPY,slopeIdx,intersecIdx,m,n);
//
//  for (int i=0;i<nContexts;i++) {
//    model[i].MPSbit=(preCtxState<=63) ? 0 : 1;
//    model[i].state = model[i].MPSbit!=0 ? (preCtxState-64) : (63-preCtxState);
//
//    // model state will always be between [0;62]
//
////    assert(model[i].state <= 62);
//  }
//}
//
//
////static int initValue_split_cu_flag[][] = new int[][]{
////  { 139,141,157 },
////  { 107,139,126 },
////  { 107,139,126 },
////};
////static int initValue_cu_skip_flag[][] = new int[][]{
////  { 197,185,201 },
////  { 197,185,201 },
////};
////static int initValue_part_mode[] = { 184,154,139, 154,154,154, 139,154,154 };
////static int initValue_prev_intra_luma_pred_flag[] = { 184,154,183 };
////static int initValue_intra_chroma_pred_mode[] = { 63,152,152 };
////static int initValue_cbf_luma[] = { 111,141,153,111 };
////static int initValue_cbf_chroma[] = { 94,138,182,154,149,107,167,154,149,92,167,154 };
////static int initValue_split_transform_flag[] = { 153,138,138, 124,138,94, 224,167,122 }; // FIX712
////static int initValue_last_significant_coefficient_prefix[] = {
////    110,110,124,125,140,153,125,127,140,109,111,143,127,111, 79,108,123, 63,
////    125,110, 94,110, 95, 79,125,111,110, 78,110,111,111, 95, 94,108,123,108,
////    125,110,124,110, 95, 94,125,111,111, 79,125,126,111,111, 79,108,123, 93
////  };
////static int initValue_coded_sub_block_flag[] = { 91,171,134,141,121,140,61,154,121,140,61,154 };
////static int initValue_significant_coeff_flag[][] = {
////    {
////      111,  111,  125,  110,  110,   94,  124,  108,  124,  107,  125,  141,  179,  153,  125,  107,
////      125,  141,  179,  153,  125,  107,  125,  141,  179,  153,  125,  140,  139,  182,  182,  152,
////      136,  152,  136,  153,  136,  139,  111,  136,  139,  111
////    },
////    {
////      155,  154,  139,  153,  139,  123,  123,   63,  153,  166,  183,  140,  136,  153,  154,  166,
////      183,  140,  136,  153,  154,  166,  183,  140,  136,  153,  154,  170,  153,  123,  123,  107,
////      121,  107,  121,  167,  151,  183,  140,  151,  183,  140,
////    },
////    {
////      170,  154,  139,  153,  139,  123,  123,   63,  124,  166,  183,  140,  136,  153,  154,  166,
////      183,  140,  136,  153,  154,  166,  183,  140,  136,  153,  154,  170,  153,  138,  138,  122,
////      121,  122,  121,  167,  151,  183,  140,  151,  183,  140
////    },
////  };
////static int initValue_significant_coeff_flag_skipmode[][] = {
////  { 141,111 }, { 140,140 }, { 140,140 }
////};
////
////static int initValue_coeff_abs_level_greater1_flag[] = {
////    140, 92,137,138,140,152,138,139,153, 74,149, 92,139,107,122,152,
////    140,179,166,182,140,227,122,197,154,196,196,167,154,152,167,182,
////    182,134,149,136,153,121,136,137,169,194,166,167,154,167,137,182,
////    154,196,167,167,154,152,167,182,182,134,149,136,153,121,136,122,
////    169,208,166,167,154,152,167,182
////  };
////static int initValue_coeff_abs_level_greater2_flag[] = {
////    138,153,136,167,152,152,107,167, 91,122,107,167,
////    107,167, 91,107,107,167
////  };
////static int initValue_sao_merge_leftUp_flag[] = { 153,153,153 };
////static int initValue_sao_type_idx_lumaChroma_flag[] = { 200,185,160 };
////static int initValue_cu_qp_delta_abs[] = { 154,154 };
////static int initValue_transform_skip_flag[] = { 139,139 };
////static int initValue_merge_flag[] = { 110,154 };
////static int initValue_merge_idx[] = { 122,137 };
////static int initValue_pred_mode_flag[] = { 149,134 };
////static int initValue_abs_mvd_greater01_flag[] = { 140,198,169,198 };
////static int initValue_mvp_lx_flag[] = { 168 };
////static int initValue_rqt_root_cbf[] = { 79 };
////static int initValue_ref_idx_lX[] = { 153,153 };
////static int initValue_inter_pred_idc[] = { 95,79,63,31,31 };
////static int initValue_cu_transquant_bypass_flag[] = { 154,154,154 };
//
//
//static void init_context(int SliceQPY, context_model[] model, int[] initValues, int len)
//{
//  for (int i=0;i<len;i++)
//    {
//      set_initValue(SliceQPY, model, initValues[i], 1);
//    }
//}
//
//
//static void init_context_const(int SliceQPY,
//                               context_model[] model,
//                               int initValue, int len)
//{
//  set_initValue(SliceQPY, model, initValue, len);
//}
//
//void initialize_CABAC_models(context_model[] context_model_table,int initType,int QPY)
//{
//  context_model[] cm = context_model_table; // just an abbreviation
//
////  if (initType > 0) {
////    init_context(QPY, cm+CONTEXT_MODEL_CU_SKIP_FLAG,    initValue_cu_skip_flag[initType-1],  3);
////    init_context(QPY, cm+CONTEXT_MODEL_PRED_MODE_FLAG, &initValue_pred_mode_flag[initType-1], 1);
////    init_context(QPY, cm+CONTEXT_MODEL_MERGE_FLAG,             &initValue_merge_flag[initType-1],1);
////    init_context(QPY, cm+CONTEXT_MODEL_MERGE_IDX,              &initValue_merge_idx[initType-1], 1);
////    init_context(QPY, cm+CONTEXT_MODEL_INTER_PRED_IDC,         initValue_inter_pred_idc,         5);
////    init_context(QPY, cm+CONTEXT_MODEL_REF_IDX_LX,             initValue_ref_idx_lX,             2);
////    init_context(QPY, cm+CONTEXT_MODEL_ABS_MVD_GREATER01_FLAG, &initValue_abs_mvd_greater01_flag[initType == 1 ? 0 : 2], 2);
////    init_context(QPY, cm+CONTEXT_MODEL_MVP_LX_FLAG,            initValue_mvp_lx_flag,            1);
////    init_context(QPY, cm+CONTEXT_MODEL_RQT_ROOT_CBF,           initValue_rqt_root_cbf,           1);
////
////    init_context_const(QPY, cm+CONTEXT_MODEL_RDPCM_FLAG, 139, 2);
////    init_context_const(QPY, cm+CONTEXT_MODEL_RDPCM_DIR,  139, 2);
////  }
////
////  init_context(QPY, cm+CONTEXT_MODEL_SPLIT_CU_FLAG, initValue_split_cu_flag[initType], 3);
////  init_context(QPY, cm+CONTEXT_MODEL_PART_MODE,     &initValue_part_mode[(initType!=2 ? initType : 5)], 4);
////  init_context(QPY, cm+CONTEXT_MODEL_PREV_INTRA_LUMA_PRED_FLAG, &initValue_prev_intra_luma_pred_flag[initType], 1);
////  init_context(QPY, cm+CONTEXT_MODEL_INTRA_CHROMA_PRED_MODE,    &initValue_intra_chroma_pred_mode[initType],    1);
////  init_context(QPY, cm+CONTEXT_MODEL_CBF_LUMA,                  &initValue_cbf_luma[initType == 0 ? 0 : 2],     2);
////  init_context(QPY, cm+CONTEXT_MODEL_CBF_CHROMA,                &initValue_cbf_chroma[initType * 4],            4);
////  init_context(QPY, cm+CONTEXT_MODEL_SPLIT_TRANSFORM_FLAG,      &initValue_split_transform_flag[initType * 3],  3);
////  init_context(QPY, cm+CONTEXT_MODEL_LAST_SIGNIFICANT_COEFFICIENT_X_PREFIX, &initValue_last_significant_coefficient_prefix[initType * 18], 18);
////  init_context(QPY, cm+CONTEXT_MODEL_LAST_SIGNIFICANT_COEFFICIENT_Y_PREFIX, &initValue_last_significant_coefficient_prefix[initType * 18], 18);
////  init_context(QPY, cm+CONTEXT_MODEL_CODED_SUB_BLOCK_FLAG,                  &initValue_coded_sub_block_flag[initType * 4],        4);
////  init_context(QPY, cm+CONTEXT_MODEL_SIGNIFICANT_COEFF_FLAG,              initValue_significant_coeff_flag[initType],    42);
////  init_context(QPY, cm+CONTEXT_MODEL_SIGNIFICANT_COEFF_FLAG+42, initValue_significant_coeff_flag_skipmode[initType], 2);
////
////  init_context(QPY, cm+CONTEXT_MODEL_COEFF_ABS_LEVEL_GREATER1_FLAG,       &initValue_coeff_abs_level_greater1_flag[initType * 24], 24);
////  init_context(QPY, cm+CONTEXT_MODEL_COEFF_ABS_LEVEL_GREATER2_FLAG,       &initValue_coeff_abs_level_greater2_flag[initType *  6],  6);
////  init_context(QPY, cm+CONTEXT_MODEL_SAO_MERGE_FLAG,                      &initValue_sao_merge_leftUp_flag[initType],    1);
////  init_context(QPY, cm+CONTEXT_MODEL_SAO_TYPE_IDX,                        &initValue_sao_type_idx_lumaChroma_flag[initType], 1);
////  init_context(QPY, cm+CONTEXT_MODEL_CU_QP_DELTA_ABS,        initValue_cu_qp_delta_abs,        2);
////  init_context(QPY, cm+CONTEXT_MODEL_TRANSFORM_SKIP_FLAG,    initValue_transform_skip_flag,    2);
////  init_context(QPY, cm+CONTEXT_MODEL_CU_TRANSQUANT_BYPASS_FLAG, &initValue_cu_transquant_bypass_flag[initType], 1);
////
////  init_context_const(QPY, cm+CONTEXT_MODEL_LOG2_RES_SCALE_ABS_PLUS1, 154, 8);
////  init_context_const(QPY, cm+CONTEXT_MODEL_RES_SCALE_SIGN_FLAG,      154, 2);
////  init_context_const(QPY, cm+CONTEXT_MODEL_CU_CHROMA_QP_OFFSET_FLAG, 154, 1);
////  init_context_const(QPY, cm+CONTEXT_MODEL_CU_CHROMA_QP_OFFSET_IDX,  154, 1);
//}
};
