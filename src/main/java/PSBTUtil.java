import java.security.interfaces.ECKey;
import java.util.*;

public class PSBTUtil {

	public static BatchTakerOfferRespDTO buildBatchOfferPSBT(BatchTakeOfferDTO dto) {
		Transaction tx = new Transaction();
		tx.setVersion(2);

		Map<Integer, UTXOInfo> inputIndexUtxoMap = new HashMap<>();
		//卖家的铭文输入，及买家的输出
		dto.getSellInscriptionInfos().forEach(info -> {
			UTXOInfo inscriptionUtxo = info.getInscriptionUtxo();
			TransactionInput input = buildTxInput(inscriptionUtxo.getTxHash(), inscriptionUtxo.getVout());
			tx.addInput(input);

			inputIndexUtxoMap.put(tx.getInputs().size() - 1, inscriptionUtxo);

			TransactionOutput output = new TransactionOutput(tx, inscriptionUtxo.getValue(), getScript(info.getOfferer()));
			tx.addOutput(output);
		});


		Script sellerFundScript = getScript(dto.getSellerFundAddress());
		final List<Integer> offerInputsIndexes = new ArrayList<>();
		dto.getOfferInfoList().forEach(offerInfo -> {
			//出价人的utxo 输入
			offerInfo.getOfferUTXOList().forEach(offerUtxo -> {
				TransactionInput input = buildTxInput(offerUtxo.getTxHash(), offerUtxo.getVout());
				tx.addInput(input);
				inputIndexUtxoMap.put(tx.getInputs().size() - 1, offerUtxo);
				offerInputsIndexes.add(tx.getInputs().size() - 1);
			});

			//卖家接收的输出
			TransactionOutput output = new TransactionOutput(tx, offerInfo.getOfferValue(), sellerFundScript);
			tx.addOutput(output);
			//处理出价人的找零
			Long totalInput = offerInfo.getOfferUTXOList().stream().map(UTXOInfo::getValue).reduce(0L, Long::sum);
			long changeValue = totalInput - offerInfo.getOfferValue();
			if (changeValue >= 546) {
				TransactionOutput changeOutput = new TransactionOutput(tx, changeValue, getScript(offerInfo.getOfferUTXOList().get(0).getOwnerAddress()));
				tx.addOutput(changeOutput);
			}
		});

		//卖家手续费输入
		dto.getSpentUTXOList().forEach(spentUtxo -> {
			TransactionInput input = buildTxInput(spentUtxo.getTxHash(), spentUtxo.getVout());
			tx.addInput(input);
			inputIndexUtxoMap.put(tx.getInputs().size() - 1, spentUtxo);
		});

		//平台费输出
		//平台费
		long platformFee = dto.getPlatformFee();
		if (platformFee > 0) {
			TransactionOutput output = new TransactionOutput(tx, platformFee, getScript(dto.getPlatformFeeReceiveAddress()));
			tx.addOutput(output);
		}

		//处理卖家手续费找零
		List<TransactionOutput> outputs = getOutputs(tx);
		Script changeScript = getScript(dto.getSpentUTXOList().get(0).getOwnerAddress());
		long tokenSpentInput = dto.getSpentUTXOList().stream().map(UTXOInfo::getValue).reduce(0L, Long::sum);
		handleChange(tx, outputs, changeScript, tokenSpentInput, platformFee, dto.getFeeRate());

		//默认签名范围为整个交易
		PSBT psbt = new PSBT(tx);
		List<PSBTInput> psbtInputs = psbt.getPsbtInputs();
		for (int i = 0; i < psbtInputs.size(); i++) {
			psbtInputs.get(i).setSigHash(SigHash.ALL);

			UTXOInfo utxoInfo = inputIndexUtxoMap.get(i);
			fillPSBTInputsOtherInfo(psbt, i, utxoInfo);
		}

		return new BatchTakerOfferRespDTO(offerInputsIndexes, psbt.toBase64String());
	}

	public static Pair<String, String> signOfferInputs(Map<Integer, String> offerIndexPrivKeyMap, String psbtStr) {
		PSBT psbt = fromPSBT(psbtStr);
		for (int i = 0; i < psbt.getPsbtInputs().size(); i++) {
			String priKey = offerIndexPrivKeyMap.get(i);
			if (Objects.isNull(priKey) || "".equals(priKey)) {
				continue;
			}
			psbt.getPsbtInputs().get(i).setSigHash(SigHash.ALL);
			byte[] priKeyBytes = Utils.isHex(priKey) ? Utils.hexToBytes(priKey) : DumpedPrivateKey.fromBase58(priKey).getKey().getPrivKeyBytes();
			ECKey ecKey = ECKey.fromPrivate(priKeyBytes).getTweakedOutputKey();
			PSBTInput psbtInput = psbt.getPsbtInputs().get(i);
			psbtInput.sign(ecKey);
			psbtInput.getPartialSignatures().put(psbtInput.getTapInternalKey(), psbtInput.getTapKeyPathSignature());
		}

		FinalizingPSBTWallet wallet = new FinalizingPSBTWallet(psbt);
		wallet.finalise(psbt);

		if (!psbt.isFinalized()) {
			throw new ServiceException("psbt not finalized.");
		}

		Transaction finalTx = psbt.extractTransaction();
		String hex = Utils.bytesToHex(finalTx.bitcoinSerialize());
		return Pair.of(finalTx.getTxId().toString(), hex);
	}
}
