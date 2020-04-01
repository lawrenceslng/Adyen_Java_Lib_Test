package com.mycompany.app;
/**
 * Hello world!
 *
 */
import com.adyen.Client;
import com.adyen.enums.Environment;
import java.math.BigDecimal;

import com.adyen.service.TerminalCloudAPI;
import io.github.cdimascio.dotenv.Dotenv;
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;

import com.adyen.model.nexo.*;

import javax.xml.datatype.*;
import java.util.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class App 
{
    public static void main( String[] args )
    {
//        System.out.println( "Hello World!" )
        Dotenv dotenv = Dotenv.load();
//        System.out.println(dotenv.get("API_KEY"));

        Client client = new Client(dotenv.get("API_KEY"),Environment.TEST);
        TerminalCloudAPI terminalcloudapi = new TerminalCloudAPI(client);


        TerminalAPIRequest newTerminalAPIReq = new TerminalAPIRequest();
        SaleToPOIRequest saletopoirequest = new SaleToPOIRequest();

        //set Message Header
        MessageHeader msgHead = new MessageHeader();
        msgHead.setProtocolVersion("3.0");
        msgHead.setMessageClass(MessageClassType.SERVICE);
        msgHead.setMessageCategory(MessageCategoryType.PAYMENT);
        msgHead.setMessageType(MessageType.REQUEST);
        msgHead.setSaleID("POSSystemJava");
        msgHead.setServiceID("SomeTest");
        msgHead.setPOIID("V400m-346388542");
        saletopoirequest.setMessageHeader(msgHead);

        //set Sale Transaction ID
        TransactionIdentification transId = new TransactionIdentification();
        transId.setTransactionID("TransID");
        //how to get xmlGregorianTime
//        transId.setTimeStamp();
        Date date = new Date();
        System.out.println("Current date: " + date);
        Instant now = date.toInstant();
        ZoneId currentZone = ZoneId.systemDefault();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(now, currentZone);
        System.out.println("Local date: " + localDateTime.withNano(0));

        String dateTimeString = now.toString();
        System.out.println(dateTimeString);
        XMLGregorianCalendar xmlGregorianCalendar;
        try{
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(dateTimeString);
            transId.setTimeStamp(xmlGregorianCalendar);
        }
        catch(Exception e){
             System.out.println(e);
        }



        //set Sale Data
        SaleData saledata = new SaleData();
        saledata.setSaleTransactionID(transId);
        //set Amount Request
        AmountsReq amountsReq = new AmountsReq();
        amountsReq.setCurrency("USD");
        amountsReq.setRequestedAmount(new BigDecimal("10.00"));
        //set Payment Transaction
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setAmountsReq(amountsReq);

        //set Payment Request
        com.adyen.model.nexo.PaymentRequest paymentRequest = new com.adyen.model.nexo.PaymentRequest();
        paymentRequest.setSaleData(saledata);
        paymentRequest.setPaymentTransaction(paymentTransaction);

        saletopoirequest.setPaymentRequest(paymentRequest);

        newTerminalAPIReq.setSaleToPOIRequest(saletopoirequest);

        try{
            TerminalAPIResponse response = terminalcloudapi.sync(newTerminalAPIReq);
            System.out.println("TAPI Response: " + response);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}


