package com.github.mkopylec.sessioncouchbase.persistent;

import org.springframework.session.ExpiringSession;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.round;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableSet;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;

public class CouchbaseSession implements ExpiringSession, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String GLOBAL_ATTRIBUTE_NAME_PREFIX = "_#global#_";
    protected static final String CREATION_TIME_ATTRIBUTE = "$creationTime";
    protected static final String LAST_ACCESSED_TIME_ATTRIBUTE = "$lastAccessedTime";
    protected static final String MAX_INACTIVE_INTERVAL_ATTRIBUTE = "$maxInactiveInterval";

    protected String id = randomUUID().toString();
    protected Map<String, Object> globalAttributes = new HashMap<>();
    protected Map<String, Object> namespaceAttributes = new HashMap<>();

    public CouchbaseSession(int timeoutInSeconds) {
        long now = currentTimeMillis();
        setCreationTime(now);
        setLastAccessedTime(now);
        setMaxInactiveIntervalInSeconds(timeoutInSeconds);
    }

    public CouchbaseSession(String id, Map<String, Object> globalAttributes, Map<String, Object> namespaceAttributes) {
        this.id = id;
        this.globalAttributes = globalAttributes == null ? new HashMap<String, Object>() : globalAttributes;
        this.namespaceAttributes = namespaceAttributes == null ? new HashMap<String, Object>() : namespaceAttributes;
    }

    public static String globalAttributeName(String attributeName) {
        return GLOBAL_ATTRIBUTE_NAME_PREFIX + attributeName;
    }

    @Override
    public long getCreationTime() {
        return round((double) globalAttributes.get(CREATION_TIME_ATTRIBUTE));
    }

    @Override
    public long getLastAccessedTime() {
        return round((double) globalAttributes.get(LAST_ACCESSED_TIME_ATTRIBUTE));
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        globalAttributes.put(LAST_ACCESSED_TIME_ATTRIBUTE, lastAccessedTime);
    }

    @Override
    public void setMaxInactiveIntervalInSeconds(int interval) {
        globalAttributes.put(MAX_INACTIVE_INTERVAL_ATTRIBUTE, interval);
    }

    @Override
    public int getMaxInactiveIntervalInSeconds() {
        return (int) globalAttributes.get(MAX_INACTIVE_INTERVAL_ATTRIBUTE);
    }

    @Override
    public boolean isExpired() {
        return getMaxInactiveIntervalInSeconds() >= 0 && currentTimeMillis() - SECONDS.toMillis(getMaxInactiveIntervalInSeconds()) >= getLastAccessedTime();
    }

    @Override
    public String getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String attributeName) {
        checkAttributeName(attributeName);
        if (isGlobal(attributeName)) {
            return (T) globalAttributes.get(getAttributeName(attributeName));
        } else {
            return (T) namespaceAttributes.get(attributeName);
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        Set<String> attributesNames = new HashSet<>();
        attributesNames.addAll(globalAttributes.keySet());
        attributesNames.addAll(namespaceAttributes.keySet());
        return unmodifiableSet(attributesNames);
    }

    @Override
    public void setAttribute(String attributeName, Object attributeValue) {
        checkAttributeName(attributeName);
        if (isGlobal(attributeName)) {
            globalAttributes.put(getAttributeName(attributeName), attributeValue);
        } else {
            namespaceAttributes.put(attributeName, attributeValue);
        }
    }

    @Override
    public void removeAttribute(String attributeName) {
        checkAttributeName(attributeName);
        if (isGlobal(attributeName)) {
            //TODO remove global attr test
            globalAttributes.remove(getAttributeName(attributeName));
        } else {
            namespaceAttributes.remove(attributeName);
        }
    }

    public Map<String, Object> getGlobalAttributes() {
        return globalAttributes;
    }

    public Map<String, Object> getNamespaceAttributes() {
        return namespaceAttributes;
    }

    protected void setCreationTime(long creationTime) {
        globalAttributes.put(CREATION_TIME_ATTRIBUTE, creationTime);
    }

    protected void checkAttributeName(String attributeName) {
        hasText(attributeName, "Empty HTTP session attribute name");
        isTrue(!attributeName.equals(GLOBAL_ATTRIBUTE_NAME_PREFIX), "Empty HTTP session global attribute name");
    }

    protected boolean isGlobal(String attributeName) {
        return attributeName.startsWith(GLOBAL_ATTRIBUTE_NAME_PREFIX);
    }

    protected String getAttributeName(String globalAttributeName) {
        return globalAttributeName.replaceFirst(GLOBAL_ATTRIBUTE_NAME_PREFIX, "");
    }
}
