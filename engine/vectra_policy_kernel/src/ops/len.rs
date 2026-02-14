use crate::{arg_or_empty, DeterministicOp, Output};

pub(crate) struct LenOp;

pub(crate) static LEN_OP: LenOp = LenOp;

impl DeterministicOp for LenOp {
    fn canonize(&self, args: &[String]) -> Vec<String> {
        vec![arg_or_empty(args, 0).to_string()]
    }

    fn execute(&self, key_args: &[String]) -> Output {
        Output::Number(arg_or_empty(key_args, 0).chars().count())
    }

    fn op_code(&self) -> &'static str {
        "len"
    }
}
