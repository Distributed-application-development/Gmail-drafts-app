package org.psu;

public class Draft {
    private String id;
    private String subject;
    private String body;

    public String getId() {
        return id;
    }

    public Draft setId(String id) {
        this.id = id;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Draft setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getBody() {
        return body;
    }

    public Draft setBody(String body) {
        this.body = body;
        return this;
    }
}
