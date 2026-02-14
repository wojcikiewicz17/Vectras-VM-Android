use crate::{arg_or_empty, first_char_or_nul, DeterministicOp, Output};

pub(crate) struct ReplaceCharOp;

pub(crate) static REPLACE_CHAR_OP: ReplaceCharOp = ReplaceCharOp;

impl DeterministicOp for ReplaceCharOp {
    fn canonize(&self, args: &[String]) -> Vec<String> {
        let source = arg_or_empty(args, 0).to_string();
        let from = first_char_or_nul(arg_or_empty(args, 1));
        let to = first_char_or_nul(arg_or_empty(args, 2));
        vec![source, from.to_string(), to.to_string()]
    }

    fn execute(&self, key_args: &[String]) -> Output {
        let source = arg_or_empty(key_args, 0);
        let from = first_char_or_nul(arg_or_empty(key_args, 1));
        let to = first_char_or_nul(arg_or_empty(key_args, 2));
        let transformed: String = source
            .chars()
            .map(|ch| if ch == from { to } else { ch })
            .collect();
        Output::Text(transformed)
    }

    fn op_code(&self) -> &'static str {
        "replace_char"
    }
}
