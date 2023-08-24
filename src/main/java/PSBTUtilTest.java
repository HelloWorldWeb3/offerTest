import java.security.interfaces.ECKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PSBTUtilTest {

	public void testBatchOffer() throws PSBTParseException {
		//你的私钥， hex 或者 WIF 私钥
		String priKey = "";
		byte[] priKeyBytes = Utils.isHex(priKey) ? Utils.hexToBytes(priKey) : DumpedPrivateKey.fromBase58(priKey).getKey().getPrivKeyBytes();
		ECKey key = ECKey.fromPrivate(priKeyBytes);

		String p2trAddress = new P2TRAddress(key.getTweakedOutputKey().getPubKeyXCoord()).getAddress();
		UTXOInfo inscriptionUtxo1 = buildUtxoInfo(p2trAddress,
				Utils.bytesToHex(key.getPubKey()), 546, 1);
		inscriptionUtxo1.setTxHash("");

		UTXOInfo inscriptionUtxo2 = buildUtxoInfo(p2trAddress,
				Utils.bytesToHex(key.getPubKey()), 546, 1);
		inscriptionUtxo2.setTxHash("");

		UTXOInfo spentUtxo = buildUtxoInfo(p2trAddress,
				Utils.bytesToHex(key.getPubKey()), 370481, 7);
		spentUtxo.setTxHash("");

		String offer = "";
		UTXOInfo offerUtxo1 = buildUtxoInfo(offer, "", 200000, 0);
		offerUtxo1.setTxHash("");

		UTXOInfo offerUtxo2 = buildUtxoInfo(offer, "", 198911, 1);
		offerUtxo2.setTxHash("");

		BatchTakeOfferDTO takeOfferDTO = BatchTakeOfferDTO.builder()
				.sellInscriptionInfos(Arrays.asList(new BatchTakeOfferDTO.SellInscriptionInfo(inscriptionUtxo1, offer), new BatchTakeOfferDTO.SellInscriptionInfo(inscriptionUtxo2, offer)))
				.spentUTXOList(Collections.singletonList(spentUtxo))
				.offerInfoList(Arrays.asList(new BatchTakeOfferDTO.OfferInfo(Collections.singletonList(offerUtxo1), 10000)
						, new BatchTakeOfferDTO.OfferInfo(Collections.singletonList(offerUtxo2), 10000)))
				.sellerFundAddress(p2trAddress)
				.sellerFundAddressPubKey(Utils.bytesToHex(key.getPubKey()))
				.platformFee(1000)
				.platformFeeReceiveAddress(p2trAddress)
				.feeRate(7)
				.build();

		BatchTakerOfferRespDTO offerRespDTO = PSBTUtil.buildBatchOfferPSBT(takeOfferDTO);
		String psbt = offerRespDTO.getBase64Psbt();
		System.out.println(offerRespDTO.getOfferInputsIndexes());

		PSBT psbt1 = PSBT.fromString(psbt);
		System.out.println(psbt1.isSigned());
		System.out.println(psbt1);


		Map<Integer, String> offerIndexPrivKeyMap = new HashMap<>();
		for (Integer offerInputsIndex : offerRespDTO.getOfferInputsIndexes()) {
			//不同的index 可能 priKey不同
			offerIndexPrivKeyMap.put(offerInputsIndex, "你的私钥");
		}

		String sellerSignPsbt = "";
		Pair<String, String> txIdTxPair = PSBTUtil.signOfferInputs(offerIndexPrivKeyMap, sellerSignPsbt);
		System.out.println();
		System.out.println(txIdTxPair.getKey());
		System.out.println(txIdTxPair.getValue());
	}
}
