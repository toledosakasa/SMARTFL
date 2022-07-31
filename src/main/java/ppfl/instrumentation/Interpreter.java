package ppfl.instrumentation;

import javassist.bytecode.Mnemonic;
import ppfl.instrumentation.opcode.*;

/*bytecode reference:
https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5*/
public class Interpreter {
	public static final OpcodeInst[] map = new OpcodeInst[256];
	private static boolean has_init = false;

	static void printopcodes() {
		init();
		for (int i = 0; i < Mnemonic.OPCODE.length; i++) {
			if (map[i] == null) {
				System.out.print("Not implemented ");
				System.out.println(i + " " + Mnemonic.OPCODE[i]);
			} else {
				// if(map[i].pushnum != 1)continue;
				System.out.print(i + " " + Mnemonic.OPCODE[i] + " ");
				System.out.println(map[i].getinst(null, 0, null));
			}
		}
	}

	public static void main(String args[]) {
		OpcodeInst.splitMethodDesc("(BIDDLjava/lang/Object;I)I");
		printopcodes();
	}

	public static void init() {
		if(has_init)
			return;
		// construct inst map based on different opcode types.
		map[0] = new NopInst(0);
		map[1] = new AconstInst(1);
		// register IconstInst here
		for (int i = 2; i <= 8; i++) {
			map[i] = new IconstInst(i, i - 3);
		}
		map[9] = new LconstInst(9, 0);
		map[10] = new LconstInst(10, 1);
		map[11] = new FconstInst(11, 0);
		map[12] = new FconstInst(12, 1);
		map[13] = new FconstInst(13, 2);
		map[14] = new DconstInst(14, 0);
		map[15] = new DconstInst(15, 1);
		map[16] = new BipushInst(16);
		map[17] = new SipushInst(17);
		for (int i = 18; i <= 20; i++) {
			map[i] = new LdcInst(i);
		}
		map[21] = new IloadInst(21);
		for (int i = 26; i <= 29; i++)
			map[i] = new Iload_NInst(i, i - 26);
		map[22] = new LloadInst(22);
		for (int i = 30; i <= 33; i++)
			map[i] = new Lload_NInst(i, i - 30);
		map[23] = new FloadInst(23);
		for (int i = 34; i <= 37; i++)
			map[i] = new Fload_NInst(i, i - 34);
		map[24] = new DloadInst(24);
		for (int i = 38; i <= 41; i++)
			map[i] = new Dload_NInst(i, i - 38);
		map[25] = new AloadInst(25);
		for (int i = 42; i <= 45; i++)
			map[i] = new Aload_NInst(i, i - 42);
		map[46] = new IaloadInst(46);
		map[47] = new LaloadInst(47);
		map[48] = new FaloadInst(48);
		map[49] = new DaloadInst(49);
		map[50] = new AaloadInst(50);
		map[51] = new BaloadInst(51);
		map[52] = new CaloadInst(52);
		map[53] = new SaloadInst(53);
		for (int i = 54; i <= 58; i++)
			map[i] = new XstoreInst(i);
		for (int i = 59; i <= 78; i++) {
			map[i] = new Xstore_NInst(i, (i + 1) % 4);// trick
		}
		for (int i = 79; i <= 86; i++)
			map[i] = new XastoreInst(i);
		map[87] = new PopInst(87);
		map[88] = new Pop2Inst(88);
		map[89] = new DupInst(89);
		map[90] = new Dup_x1Inst(90);
		map[91] = new Dup_x2Inst(91);
		map[92] = new Dup2Inst(92);
		map[93] = new Dup2_x1Inst(93);
		map[94] = new Dup2_x2Inst(94);
		map[95] = new SwapInst(95);
		for (int i = 96; i <= 112; i += 4) {
			map[i] = new IarithInst(i);
		}
		for (int i = 97; i <= 113; i += 4) {
			map[i] = new LarithInst(i);
		}
		for (int i = 98; i <= 114; i += 4) {
			map[i] = new FarithInst(i);
		}
		for (int i = 99; i <= 115; i += 4) {
			map[i] = new DarithInst(i);
		}
		map[116] = new InegInst(116);
		map[117] = new LnegInst(117);
		map[118] = new FnegInst(118);
		map[119] = new DnegInst(119);
		for (int i = 120; i <= 124; i += 2) {
			map[i] = new IarithInst(i);
		}
		for (int i = 121; i <= 125; i += 2) {
			map[i] = new LarithInst(i);
		}
		for (int i = 126; i <= 130; i += 2) {
			map[i] = new IlogicInst(i);
		}
		for (int i = 127; i <= 131; i += 2) {
			map[i] = new LlogicInst(i);
		}
		map[132] = new IincInst(132);
		for (int i = 136; i <= 142; i += 3) {
			map[i] = new IturntypeInst(i);
		}
		map[133] = new LturntypeInst(133);
		map[134] = new FturntypeInst(134);
		map[137] = new FturntypeInst(137);
		map[140] = new LturntypeInst(140);
		map[143] = new LturntypeInst(143);
		map[144] = new FturntypeInst(144);
		for (int i = 135; i <= 141; i += 3) {
			map[i] = new DturntypeInst(i);
		}
		map[145] = new I2bInst(145);
		map[146] = new I2cInst(146);
		map[147] = new I2sInst(147);

		map[148] = new LcmpInst(148);
		for (int i = 149; i <= 150; i++) {
			map[i] = new FcmpInst(i);
		}
		for (int i = 151; i <= 152; i++) {
			map[i] = new DcmpInst(i);
		}
		for (int i = 153; i <= 158; i++) {
			map[i] = new IfInst(i);
		}
		for (int i = 159; i <= 164; i++) {
			map[i] = new If_icmpInst(i);
		}
		for (int i = 165; i <= 166; i++) {
			map[i] = new If_acmpInst(i);
		}
		map[167] = new GotoInst(167);
		map[168] = new JsrInst(168);
		map[169] = new RetInst(169);
		map[170] = new TableSwitchInst(170);
		map[171] = new LookupSwitchInst(171);
		map[172] = new IreturnInst(172);
		map[173] = new LreturnInst(173);
		map[174] = new FreturnInst(174);
		map[175] = new DreturnInst(175);
		map[176] = new AreturnInst(176);
		map[177] = new ReturnInst(177);
		map[178] = new GetStaticInst(178);
		map[179] = new PutStaticInst(179);
		map[180] = new GetFieldInst(180);
		map[181] = new PutFieldInst(181);
		map[182] = new InvokevirtualInst(182);
		map[183] = new InvokespecialInst(183);
		map[184] = new InvokestaticInst(184);
		map[185] = new InvokeinterfaceInst(185);
		map[186] = new InvokedynamicInst(186);
		map[187] = new NewInst(187);
		map[188] = new NewArrayInst(188);
		map[189] = new AnewArrayInst(189);
		map[190] = new ArrayLengthInst(190);
		map[191] = new AthrowInst(191);
		map[192] = new CheckCastInst(192);
		map[193] = new InstanceOfInst(193);
		map[194] = new MonitorEnterInst(194);
		map[195] = new MonitorExitInst(195);
		map[196] = new WideInst(196);
		map[197] = new MultiAnewArrayInst(197);
		map[198] = new IfnullInst(198);
		map[199] = new IfnonnullInst(199);
		map[200] = new Goto_wInst(200);
		map[201] = new Jsr_wInst(201);

		// for compromising
		map[255] = new OpcodeInst(255, 1, 1);

		has_init = true;
	}
}
