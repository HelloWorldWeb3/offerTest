import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchTakeOfferDTO {
	/**
	 * 用于支付的 utxo 列表
	 */
	private List<UTXOInfo> spentUTXOList;
	/**
	 * 卖家资金地址，支持独立设置
	 */
	private String sellerFundAddress;
	/**
	 * 卖家资金地址对应的公钥
	 */
	private String sellerFundAddressPubKey;
	/**
	 * 网络费用 如 25/vByte
	 */
	private long feeRate;
	/**
	 * 平台费，单位：聪
	 */
	private long platformFee;
	/**
	 * 平台费接收地址
	 */
	private String platformFeeReceiveAddress;
	/**
	 * 卖出铭文的utxo及对应的出价地址
	 */
	private List<SellInscriptionInfo> sellInscriptionInfos;
	/**
	 * 每个出价人需要支付给卖家的UTXO列表；
	 */
	private List<OfferInfo> offerInfoList;

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SellInscriptionInfo {
		private UTXOInfo inscriptionUtxo;
		/**
		 * 出价人接收铭文的地址
		 */
		private String offerer;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OfferInfo {
		/**
		 * 出价人的UTXO列表
		 */
		private List<UTXOInfo> offerUTXOList;
		/**
		 * 出价金额
		 */
		private long offerValue;
	}
}
