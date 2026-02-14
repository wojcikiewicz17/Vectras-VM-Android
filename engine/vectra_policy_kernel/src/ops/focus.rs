use crate::{arg_or_empty, DeterministicOp, Output};

pub(crate) struct FocusOp;

pub(crate) static FOCUS_OP: FocusOp = FocusOp;

impl DeterministicOp for FocusOp {
    fn canonize(&self, args: &[String]) -> Vec<String> {
        vec![arg_or_empty(args, 0).trim().to_string()]
    }

    fn execute(&self, key_args: &[String]) -> Output {
        Output::Focus(arg_or_empty(key_args, 0).to_string())
    }

    fn op_code(&self) -> &'static str {
        "focus"
    }
}
