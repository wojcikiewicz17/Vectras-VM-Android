use crate::{arg_or_empty, AnchorAddr, DeterministicOp, Output};

pub(crate) struct AnchorOp;

pub(crate) static ANCHOR_OP: AnchorOp = AnchorOp;

fn parse_anchor_addr(raw: &str) -> AnchorAddr {
    let mut parts = raw.split(':');
    let dev = parts
        .next()
        .and_then(|v| v.trim().parse::<u16>().ok())
        .unwrap_or(0);
    let block = parts
        .next()
        .and_then(|v| v.trim().parse::<u64>().ok())
        .unwrap_or(0);
    let page = parts
        .next()
        .and_then(|v| v.trim().parse::<u16>().ok())
        .unwrap_or(0);

    AnchorAddr { dev, block, page }
}

impl DeterministicOp for AnchorOp {
    fn canonize(&self, args: &[String]) -> Vec<String> {
        vec![arg_or_empty(args, 0).trim().to_string()]
    }

    fn execute(&self, key_args: &[String]) -> Output {
        let anchor = parse_anchor_addr(arg_or_empty(key_args, 0));
        Output::Anchor(anchor)
    }

    fn op_code(&self) -> &'static str {
        "anchor"
    }
}
