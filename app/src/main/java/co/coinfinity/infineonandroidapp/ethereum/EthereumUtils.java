package co.coinfinity.infineonandroidapp.ethereum;

import android.nfc.Tag;
import android.util.Log;
import co.coinfinity.infineonandroidapp.ethereum.bean.EthBalanceBean;
import co.coinfinity.infineonandroidapp.nfc.NfcUtils;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

public class EthereumUtils {

    public static EthBalanceBean getBalance(String ethAddress) {
        // connect to node
        Web3j web3 = Web3jFactory.build(new HttpService("https://ropsten.infura.io/v3/7b40d72779e541a498cb0da69aa418a2"));

        BigInteger wei = getBalanceFromApi(web3, ethAddress, DefaultBlockParameterName.LATEST);
        BigDecimal ether = Convert.fromWei(wei.toString(), Convert.Unit.ETHER);
        Log.d("WEB3J", ether + " Ether");
        Log.d("WEB3J", wei + " Wei");

        BigInteger unconfirmedWei = getBalanceFromApi(web3, ethAddress, DefaultBlockParameterName.PENDING);
        BigDecimal unconfirmedEther = Convert.fromWei(unconfirmedWei.toString(), Convert.Unit.ETHER);
        Log.d("WEB3J", unconfirmedWei + " Ether unconfirmed");
        Log.d("WEB3J", unconfirmedEther + " Wei unconfirmed");

        return new EthBalanceBean(wei,ether,unconfirmedWei,unconfirmedEther);
    }

    private static BigInteger getBalanceFromApi(Web3j web3, String ethAddress, DefaultBlockParameterName defaultBlockParameterName) {
        BigInteger wei = null;
        // send synchronous requests to get balance
        try {
            EthGetBalance ethGetBalance = web3
                    .ethGetBalance(ethAddress, defaultBlockParameterName)
                    .send();

            if(ethGetBalance != null) {
                wei = ethGetBalance.getBalance();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wei;
    }

    public static void sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String from, String to, BigInteger value, Tag tagFromIntent) {

        // connect to node
        Web3j web3 = Web3jFactory.build(new HttpService("https://ropsten.infura.io/v3/7b40d72779e541a498cb0da69aa418a2"));

        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                getNextNonce(web3, from), gasPrice, gasLimit, to, value);


        //SIGN transaction
        String signedMessage = null;
        try {
//            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, <credentials>);
            signedMessage = NfcUtils.signTransaction(tagFromIntent, 0x01, rawTransaction.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        EthSendTransaction ethSendTransaction = null;
        try {
            ethSendTransaction = web3.ethSendRawTransaction(signedMessage).sendAsync().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String transactionHash = ethSendTransaction.getTransactionHash();
        Log.d("WEB3J", "TransactionHash: " + transactionHash);
        // poll for transaction response via org.web3j.protocol.Web3j.ethGetTransactionReceipt(<txHash>)

    }

    private static BigInteger getNextNonce(Web3j web3j, String etherAddress) {
        EthGetTransactionCount ethGetTransactionCount = null;
        try {
            ethGetTransactionCount = web3j.ethGetTransactionCount(
                    etherAddress, DefaultBlockParameterName.LATEST).send();

            return ethGetTransactionCount.getTransactionCount();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
