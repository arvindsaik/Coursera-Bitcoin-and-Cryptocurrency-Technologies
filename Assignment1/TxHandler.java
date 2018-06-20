import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double sumInputs = 0;
        double sumOutputs = 0;
        for(int j = 0; j < tx.numInputs(); ++j){
            UTXO tempUTXO = new UTXO(tx.getInput(j).prevTxHash,tx.getInput(j).outputIndex);
            if(utxoPool.contains(tempUTXO) == false){
                return false;
            }
            if(Crypto.verifySignature(utxoPool.getTxOutput(tempUTXO).address, tx.getRawDataToSign(j), tx.getInput(j).signature) == false){
                return false;
            }
            int count = 0;
            for(Transaction.Input t : tx.getInputs()){
                UTXO tempUTXO2 = new UTXO(t.prevTxHash,t.outputIndex);
                if(tempUTXO.equals(tempUTXO2)){
                    count++;
                }
            }
            if(count > 1){
                return false;
            }
            Transaction.Output output = utxoPool.getTxOutput(tempUTXO);
            sumInputs = sumInputs + output.value;            
        }
        for (Transaction.Output outputNew : tx.getOutputs()) {
            if (outputNew.value < 0) return false;
            sumOutputs = sumOutputs + outputNew.value;
        }
        if(sumInputs < sumOutputs){
            return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();        
        for(int i = 0; i < possibleTxs.length; ++i){
            if(isValidTx(possibleTxs[i])){
                validTxs.add(possibleTxs[i]);
                for(int j = 0; j < possibleTxs[i].numInputs(); ++j){
                    UTXO tempUTXO = new UTXO(possibleTxs[i].getInput(j).prevTxHash,possibleTxs[i].getInput(j).outputIndex);
                    utxoPool.removeUTXO(tempUTXO);
                }
                for(int j = 0; j < possibleTxs[i].numOutputs(); ++j){
                    UTXO tempUTXO = new UTXO(possibleTxs[i].getHash(),j);
                    Transaction.Output tempTxOutput = possibleTxs[i].getOutput(j);
                    utxoPool.addUTXO(tempUTXO,tempTxOutput);
                }
            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
