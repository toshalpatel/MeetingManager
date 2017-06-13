package com.ibm.watson.developer_cloud.android.examples;

/**
 * Created by Toshal Patel on 18-05-2017.
 */

public class MeetingDetails {
    String topic, venue, remark;

    public MeetingDetails(String venue, String topic, String remark) {
        this.venue = venue;
        this.topic = topic;
        this.remark = remark;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getVenue() {
        return venue;
    }

    public void setDetails(String details) {
        this.venue = venue;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}