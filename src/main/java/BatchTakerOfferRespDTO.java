import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchTakerOfferRespDTO {
	/**
	 * 出价utxo 所在的下标列表
	 */
	private List<Integer> offerInputsIndexes;

	private String base64Psbt;
}
