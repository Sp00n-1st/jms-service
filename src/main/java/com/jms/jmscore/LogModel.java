package com.jms.jmscore;

import java.util.Date;

public class LogModel {
    private String jmsId;
    private Date dateTime;
    private String message;
    private String eksReference;
    private String amqCid;
    private String amqOrigQueue;
    private String amqOrigAddress;
    private Long amqOrigMessageId;
    private Long hdrBrokerInTime;
    private String hdrMessageId;
    private String reasonError;
    private Date createdAt;

    public LogModel() {
    }

    public LogModel(String jmsId, Date dateTime, String message, String eksReference, String amqCid,
            String amqOrigQueue, String amqOrigAddress, Long amqOrigMessageId,
            Long hdrBrokerInTime, String hdrMessageId, String reasonError, Date createdAt) {
        this.jmsId = jmsId;
        this.dateTime = dateTime;
        this.message = message;
        this.eksReference = eksReference;
        this.amqCid = amqCid;
        this.amqOrigQueue = amqOrigQueue;
        this.amqOrigAddress = amqOrigAddress;
        this.amqOrigMessageId = amqOrigMessageId;
        this.hdrBrokerInTime = hdrBrokerInTime;
        this.hdrMessageId = hdrMessageId;
        this.reasonError = reasonError;
        this.createdAt = createdAt;
    }

    public String getJmsId() {
        return jmsId;
    }

    public void setJmsId(String jmsId) {
        this.jmsId = jmsId;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEksReference() {
        return eksReference;
    }

    public void setEksReference(String eksReference) {
        this.eksReference = eksReference;
    }

    public String getAmqCid() {
        return amqCid;
    }

    public void setAmqCid(String amqCid) {
        this.amqCid = amqCid;
    }

    public String getAmqOrigQueue() {
        return amqOrigQueue;
    }

    public void setAmqOrigQueue(String amqOrigQueue) {
        this.amqOrigQueue = amqOrigQueue;
    }

    public String getAmqOrigAddress() {
        return amqOrigAddress;
    }

    public void setAmqOrigAddress(String amqOrigAddress) {
        this.amqOrigAddress = amqOrigAddress;
    }

    public Long getAmqOrigMessageId() {
        return amqOrigMessageId;
    }

    public void setAmqOrigMessageId(Long amqOrigMessageId) {
        this.amqOrigMessageId = amqOrigMessageId;
    }

    public Long getHdrBrokerInTime() {
        return hdrBrokerInTime;
    }

    public void setHdrBrokerInTime(Long hdrBrokerInTime) {
        this.hdrBrokerInTime = hdrBrokerInTime;
    }

    public String getHdrMessageId() {
        return hdrMessageId;
    }

    public void setHdrMessageId(String hdrMessageId) {
        this.hdrMessageId = hdrMessageId;
    }

    public String getReasonError() {
        return reasonError;
    }

    public void setReasonError(String reasonError) {
        this.reasonError = reasonError;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
