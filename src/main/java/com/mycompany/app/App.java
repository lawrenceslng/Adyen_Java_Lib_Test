package com.mycompany.app;
/**
 * Hello world!
 *
 */
import com.adyen.Client;
import com.adyen.enums.Environment;
import java.math.BigDecimal;

import com.adyen.model.modification.ModificationResult;
import com.adyen.model.terminal.SaleToAcquirerData;
import com.adyen.service.Modification;
import com.adyen.service.TerminalCloudAPI;
import io.github.cdimascio.dotenv.Dotenv;
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;
import com.adyen.model.modification.AdjustAuthorisationRequest;

import com.adyen.model.nexo.*;

import javax.xml.datatype.*;
import java.util.Base64;
import java.util.GregorianCalendar;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Scanner;


public class App {
    private static String protocolV = "3.0";
    private static String saleId = "POSSystemJava";
    private static int randomNum = ThreadLocalRandom.current().nextInt(0, 1000000000);
    private static String serviceId = Integer.toString(randomNum);
    private static String transactionId = "LNT_Java";
    private static String poiId = "V400m-346388542";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Start of Main");
        Dotenv dotenv = Dotenv.load();
        Scanner menu = new Scanner(System.in);
        System.out.println("Demo Payment Flow with Java API Library");
        System.out.println("Enter 1 for normal payment, 2 for Pre-Authorisation");
        String choice = menu.nextLine();
        Client client = new Client(dotenv.get("API_KEY"), Environment.TEST);
        TerminalAPIResponse terminalAPIResponse;
        switch(choice){
            case "1":
                System.out.println("normal payment starting...");
                terminalAPIResponse = regularPayment(client);
                break;
            case "2":
                System.out.println("pre-auth payment starting...");
                terminalAPIResponse = preAuthPayment(client);
                break;
            default:
                System.out.println("No selection, exiting...");
                return;
        }
        String tenderReference;
        String pspReference;
        String adjustAuthData;
        String result;
        if(terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getPOIData().getPOITransactionID().getTransactionID().length() < 20){
            System.out.println("Some error happening, exiting...");
            return;
        }
//        System.out.println(terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getPOIData().getPOITransactionID().getTransactionID());
        tenderReference = terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getPOIData().getPOITransactionID().getTransactionID().substring(0,19);
        pspReference = terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getPOIData().getPOITransactionID().getTransactionID().substring(20);
        Base64.Decoder decoder = Base64.getDecoder();
        String decodedData = new String(decoder.decode(terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse()));
        System.out.println("Decoded Data: " + decodedData);
        String[] tokens = decodedData.split(",");
        for (int i = 0; i < tokens.length; i++){
            if(tokens[i].contains("adjustAuthorisationData")) {
                String[] parsed = tokens[i].split("\"");
                System.out.println(parsed[3]);
                System.out.println(parsed[3].replaceAll("\\",""));
                adjustAuthData = parsed[3];
                System.out.println(adjustAuthData);
            }
        }

        adjustAuthData = "";
        result = terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult().toString();
//        System.out.println(tenderReference);
//        System.out.println(pspReference);
//        System.out.println(adjustAuthData);
        System.out.println(result);
        System.out.println("Press 1 to do Auth Adjust (Async), Press 2 to do Auth Adjust (Sync)");
        choice = menu.nextLine();
        ModificationResult modificationResult;
        switch(choice) {
            case "1":
                modificationResult = authAdjustReqAsync(client, pspReference);
                break;
            case "2":
                System.out.println("WIP");
                modificationResult = authAdjustReqSync(client, pspReference, adjustAuthData);
                return;
            default:
                System.out.println("Exiting...");
                return;
        }
        if(modificationResult != null)
        {
            System.out.println(modificationResult.getResponse());
        }

//
//        Base64.Decoder decoder = Base64.getDecoder();
//        String decodedData = new String(decoder.decode(terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse()));
//        System.out.println("Decoded Data: " + decodedData);
//        String[] tokens = decodedData.split(",");
//        String adjustAuthData = "test";
//        for (int i = 0; i < tokens.length; i++){
//            if(tokens[i].contains("adjustAuthorisationData")) {
//                String[] parsed = tokens[i].split("\"");
//                System.out.println(parsed[3]);
//                System.out.println(parsed[3].replaceAll("\\",""));
//                adjustAuthData = parsed[3];
//                System.out.println(adjustAuthData);
//            }
//        }

        System.out.println("End");
    }

    private static TerminalAPIResponse regularPayment(Client client) {
        TerminalCloudAPI terminalcloudapi = new TerminalCloudAPI(client);
        TerminalAPIRequest newTerminalAPIReq = new TerminalAPIRequest();
        SaleToPOIRequest saletopoirequest = new SaleToPOIRequest();

        //set Message Header
        MessageHeader msgHead = new MessageHeader();
        msgHead.setProtocolVersion(protocolV);
        msgHead.setMessageClass(MessageClassType.SERVICE);
        msgHead.setMessageCategory(MessageCategoryType.PAYMENT);
        msgHead.setMessageType(MessageType.REQUEST);
        msgHead.setSaleID(saleId);
        msgHead.setServiceID(serviceId);
        msgHead.setPOIID(poiId);
        saletopoirequest.setMessageHeader(msgHead);

        //set Sale Transaction ID
        TransactionIdentification transId = new TransactionIdentification();
        transId.setTransactionID(transactionId);
        //how to get xmlGregorianTime
        try {
            transId.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (Exception e) {
            System.out.println(e);
        }

        //set Sale Data
        SaleData saledata = new SaleData();
        saledata.setSaleTransactionID(transId);
        //set Amount Request
        AmountsReq amountsReq = new AmountsReq();
        amountsReq.setCurrency("USD");
        amountsReq.setRequestedAmount(BigDecimal.valueOf(10.99));
        //set Payment Transaction
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setAmountsReq(amountsReq);

        //set Payment Request
        com.adyen.model.nexo.PaymentRequest paymentRequest = new com.adyen.model.nexo.PaymentRequest();
        paymentRequest.setSaleData(saledata);
        paymentRequest.setPaymentTransaction(paymentTransaction);

        saletopoirequest.setPaymentRequest(paymentRequest);

        newTerminalAPIReq.setSaleToPOIRequest(saletopoirequest);

        try {
            TerminalAPIResponse newTerminalAPIRes = terminalcloudapi.sync(newTerminalAPIReq);
//            System.out.println("TAPI Response: " + newTerminalAPIRes.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult());
            return newTerminalAPIRes;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    private static TerminalAPIResponse preAuthPayment(Client client) {
        TerminalCloudAPI terminalcloudapi = new TerminalCloudAPI(client);
        TerminalAPIRequest newTerminalAPIReq = new TerminalAPIRequest();
        SaleToPOIRequest saletopoirequest = new SaleToPOIRequest();

        //set Message Header
        MessageHeader msgHead = new MessageHeader();
        msgHead.setProtocolVersion(protocolV);
        msgHead.setMessageClass(MessageClassType.SERVICE);
        msgHead.setMessageCategory(MessageCategoryType.PAYMENT);
        msgHead.setMessageType(MessageType.REQUEST);
        msgHead.setSaleID(saleId);
        msgHead.setServiceID(serviceId);
        msgHead.setPOIID(poiId);
        saletopoirequest.setMessageHeader(msgHead);

        //set Sale Transaction ID
        TransactionIdentification transId = new TransactionIdentification();
        transId.setTransactionID(transactionId);
        //how to get xmlGregorianTime
        try {
            transId.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (Exception e) {
            System.out.println(e);
        }

        //set SaletoAcquirerData
        SaleToAcquirerData saleToAcquirerData = new SaleToAcquirerData();
        Map<String, String> newMap = new HashMap<String, String>();
        newMap.put("authorisationType", "PreAuth");
        saleToAcquirerData.setAdditionalData(newMap);

        //set Sale Data
        SaleData saledata = new SaleData();
        saledata.setSaleTransactionID(transId);
        saledata.setSaleToAcquirerData(saleToAcquirerData);
        //set Amount Request
        AmountsReq amountsReq = new AmountsReq();
        amountsReq.setCurrency("USD");
        amountsReq.setRequestedAmount(BigDecimal.valueOf(10.99));
        //set Payment Transaction
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setAmountsReq(amountsReq);

        //set Payment Request
        com.adyen.model.nexo.PaymentRequest paymentRequest = new com.adyen.model.nexo.PaymentRequest();
        paymentRequest.setSaleData(saledata);
        paymentRequest.setPaymentTransaction(paymentTransaction);

        saletopoirequest.setPaymentRequest(paymentRequest);

        newTerminalAPIReq.setSaleToPOIRequest(saletopoirequest);
        try {
            TerminalAPIResponse newTerminalAPIRes = terminalcloudapi.sync(newTerminalAPIReq);
            return newTerminalAPIRes;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    private static ModificationResult authAdjustReqAsync(Client client, String pspReference) {
        AdjustAuthorisationRequest adjustAuthReq = new AdjustAuthorisationRequest();
        com.adyen.model.Amount amount = new com.adyen.model.Amount();
        amount.setCurrency("USD");
        amount.setValue((long) 1200);
        adjustAuthReq.setModificationAmount(amount);
        adjustAuthReq.setMerchantAccount("LN_Test_Account");
        adjustAuthReq.setOriginalReference(pspReference);
        adjustAuthReq.setReference("Auth_Adjust");
        Map<String, String> adjustAuthMap = new HashMap<String, String>();
        adjustAuthMap.put("industryUsage", "DelayedCharge");
        adjustAuthReq.setAdditionalData(adjustAuthMap);
        Modification mod = new Modification(client);
        try {
            ModificationResult modRes = mod.adjustAuthorization(adjustAuthReq);
            return modRes;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    private static ModificationResult authAdjustReqSync(Client client, String pspReference, String adjustAuthData) {

        return null;
    }
}



