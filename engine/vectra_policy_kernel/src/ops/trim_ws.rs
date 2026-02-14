use crate::{arg_or_empty, DeterministicOp, Output};

pub(crate) struct TrimWsOp;

pub(crate) static TRIM_WS_OP: TrimWsOp = TrimWsOp;

impl DeterministicOp for TrimWsOp {
    fn canonize(&self, args: &[String]) -> Vec<String> {
        vec![arg_or_empty(args, 0).trim().to_string()]
    }

    fn execute(&self, key_args: &[String]) -> Output {
        Output::Text(arg_or_empty(key_args, 0).to_string())
    }

    fn op_code(&self) -> &'static str {
        "trim_ws"
    }
}
