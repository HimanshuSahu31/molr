/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.molr.type;

/**
 * {@link Ack} is often used as an acknowledgement return type ( instead of void :) )
 * 
 * @author nachivpn
 * @author yassine
 */
public class Ack{

    private String message = "ACK";
    
    public Ack() {}

    public Ack(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    
}
