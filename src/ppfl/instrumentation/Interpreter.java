package ppfl.instrumentation;

import javassist.bytecode.Mnemonic;
import ppfl.instrumentation.OpcodeInst.paratype;

public class Interpreter {
	static OpcodeInst map[] = new OpcodeInst[256];
	// loads
	static int[] loadvar_para = { 21, 25 };// load 1 var, index at byte(pos + 1)
	static int[] loadvar_opcode = { 26, 42 };// load 1 var, index based on (opcode-base_opcode)
	static int[] loadvar_opcode_cnt = { 3, 3 };// the length of loadvar_opcode
	static int[] loadconst_para = { 16,17 };// load 1 const,value at byte(pos + 1)
	static int[] loadconst_opcode = { 2, 9, 11, 14 };// load 1 const, index based on (opcode-base_opcode)
	static int[] loadconst_opcode_cnt = { 6, 1, 2, 1 };// the length of loadconst_opcode
	// stores
	static int[] storevar_para = { 54 };// store(pop) 1 value at localvar, index at byte(pos + 1)
	static int[] storevar_opcode = { 59 };// store 1 value at localvar, index based on opcode
	static int[] storevar_opcode_cnt = { 3 };// length
	// pops
	static int[] pop1 = { 87 };// pop 1
	static int[] pop2 = { 88 };// pop 2
	// branching
	static int[] branch_pop1 = { 153, 154, 155, 156, 157, 158 };// branching that pops 1
	static int[] branch_pop2 = { 159, 160, 161, 162, 163, 164 };// pops 2
	// controls
	static int[] neglect = { 167 };// do nothing to the stack
	static int[] ret0 = { 177 };// return void. pop all things in current frame.
	static int[] ret1 = { 172, 175 };// return 1 value. push 1 after pop all
	// invokes
	static int[] invokes = { 182, 184 };// invokes. parameters maybe parsed from source? TODO
	// arith
	static int[] pop2push1 = { 96, 100, 104, 108 };// pop2 push1 arith(most common, like +-*/)
	static int[] pop1push1 = { 116, 133, 134, 135, 145, 146, 147 };// pop1 push1 (negate)
	static int[] para1const1 = { 132 };// special(iinc)

	static void printopcodes() {
		for (int i = 0; i < Mnemonic.OPCODE.length; i++)
			System.out.println(i + " " + Mnemonic.OPCODE[i]);

	}

	public static void main(String args[]) {
		printopcodes();
	}

	static void init() {
		// construct inst map based on different opcode types.
		// loads
		// aconst_null
		map[0] = new OpcodeInst(0, 1, 0);
		map[0].setPush(paratype.CONST, "NULL");
		for (int id : loadvar_para) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPara(0, paratype.PARAVAR);
			map[id].setPush(paratype.PARAVAR, null);
		}
		for (int id : loadconst_para) {
			map[id] = new OpcodeInst(id, 1, 0);
			map[id].setPara(0, paratype.PARACONST);
			map[id].setPush(paratype.PARACONST, null);
		}
		assert (loadvar_opcode.length == loadvar_opcode_cnt.length);
		for (int i = 0; i < loadvar_opcode.length; i++) {
			for (int j = 0; j <= loadvar_opcode_cnt[i]; j++) {
				int id = loadvar_opcode[i] + j;
				map[id] = new OpcodeInst(id, 1, 0);
				map[id].setPush(paratype.VAR, String.valueOf(j));
			}
		}
		assert (loadconst_opcode.length == loadconst_opcode_cnt.length);
		for (int i = 0; i < loadconst_opcode.length; i++) {
			for (int j = 0; j <= loadconst_opcode_cnt[i]; j++) {
				int id = loadconst_opcode[i] + j;
				map[id] = new OpcodeInst(id, 1, 0);
				int bias = i == 0 ? j - 1 : j;//iconst_<n> started from m1
				map[id].setPush(paratype.CONST, String.valueOf(bias));
			}
		}
		// stores
		for (int id : storevar_para) {
			map[id] = new OpcodeInst(id, 0, 1);
			map[id].setStore(paratype.PARAVAR, null);
		}
		assert (storevar_opcode.length == storevar_opcode_cnt.length);
		for (int i = 0; i < storevar_opcode.length; i++) {
			for (int j = 0; j <= storevar_opcode_cnt[i]; j++) {
				int id = storevar_opcode[i] + j;
				map[id] = new OpcodeInst(id, 0, 1);
				map[id].setPush(paratype.VAR, String.valueOf(j));
			}
		}
		// pops
		for (int id : pop1) {
			map[id] = new OpcodeInst(id, 0, 1);
		}
		for (int id : pop2) {
			map[id] = new OpcodeInst(id, 0, 2);
		}
		// branching
		for (int id : branch_pop1) {
			map[id] = new OpcodeInst(id, 0, 1);
		}
		for (int id : branch_pop2) {
			map[id] = new OpcodeInst(id, 0, 2);
		}
		// controls
		for (int id : neglect) {
			map[id] = new OpcodeInst(id, 0, 0);
		}
		for (int id : ret0) {
			map[id] = new OpcodeInst(id, 0, -1);
		}
		for (int id : ret1) {
			map[id] = new OpcodeInst(id, 1, -1);
		}
		// invokes
		for (int id : invokes) {
			map[id] = new OpcodeInst(id, 0, 0);
			// TODO set invoke
		}
		// arith
		for (int id : pop2push1) {
			map[id] = new OpcodeInst(id, 1, 2);
		}
		for (int id : pop1push1) {
			map[id] = new OpcodeInst(id, 1, 1);
		}
		for (int id : para1const1) {
			map[id] = new OpcodeInst(id, 0, 0);
			map[id].setPara(0, paratype.PARAVAR);
			map[id].setPara(1, paratype.PARACONST);
		}
	}
}
