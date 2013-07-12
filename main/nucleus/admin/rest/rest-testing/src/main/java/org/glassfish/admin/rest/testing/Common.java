package org.glassfish.admin.rest.testing;

import java.util.Iterator;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class Common {
    public static final int SC_OK = Status.OK.getStatusCode();
    public static final int SC_CREATED = Status.CREATED.getStatusCode();
    public static final int SC_ACCEPTED = Status.ACCEPTED.getStatusCode();
    public static final int SC_NOT_FOUND = Status.NOT_FOUND.getStatusCode();
    public static final int SC_BAD_REQUEST = Status.BAD_REQUEST.getStatusCode();
    public static final int SC_UNAUTHORIZED = Status.UNAUTHORIZED.getStatusCode();
    public static final int SC_ANY = -1;
    
    // Commonly used property names:
    public static final String PROP_ITEM = "item";
    public static final String PROP_ITEMS = "items";
    public static final String PROP_RESOURCES = "resources";
    public static final String PROP_MESSAGES = "messages";
    // Commonly used resource relationship names:
    public static final String REL_ACTION = "action";
    public static final String REL_PARENT = "parent";
    // Commonly used query parameter names:
    public static final String QUERY_DETACHED = "__detached";
    public static final String QUERY_INCLUDE = "__includeFields";
    public static final String QUERY_EXCLUDE = "__excludeFields";

    public static boolean haveValue(String val) {
        return (val != null && val.length() > 0);
    }

    public static ObjectValue objectVal() {
        return new ObjectValue();
    }

    public static ArrayValue arrayVal() {
        return new ArrayValue();
    }

    public static StringValue stringVal() {
        return new StringValue();
    }

    public static StringValue stringVal(String value) {
        return (StringValue) stringVal().value(value);
    }

    public static StringValue stringRegexp(String regexp) {
        return (StringValue) stringVal().regexp(regexp);
    }

    public static StringValue anyString() {
        return stringRegexp(".*");
    }

    public static StringValue regexp(String regexp) {
        return stringRegexp(regexp);
    } // Most of the time regular expressions are used for strings

    public static LongValue longVal() {
        return new LongValue();
    }

    public static LongValue longVal(long value) {
        return (LongValue) longVal().value(value);
    }

    public static LongValue longRegexp(String regexp) {
        return (LongValue) longVal().regexp(regexp);
    }

    public static LongValue anyLong() {
        return longRegexp(".*");
    } // TBD - write a better regexp

    public static IntValue intVal() {
        return new IntValue();
    }

    public static IntValue intVal(int value) {
        return (IntValue) intVal().value(value);
    }

    public static IntValue intRegexp(String regexp) {
        return (IntValue) intVal().regexp(regexp);
    }

    public static IntValue anyInt() {
        return intRegexp(".*");
    } // TBD - write a better regexp

    public static DoubleValue doubleVal() {
        return new DoubleValue();
    }

    public static DoubleValue doubleVal(double value) {
        return (DoubleValue) doubleVal().value(value);
    }

    public static DoubleValue doubleRegexp(String regexp) {
        return (DoubleValue) doubleVal().regexp(regexp);
    }

    public static DoubleValue anyDouble() {
        return doubleRegexp(".*");
    } // TBD - write a better regexp

    public static BooleanValue booleanVal() {
        return new BooleanValue();
    }

    public static BooleanValue booleanVal(boolean value) {
        return (BooleanValue) booleanVal().value(value);
    }

    public static BooleanValue booleanRegexp(String regexp) {
        return (BooleanValue) booleanVal().regexp(regexp);
    }

    public static BooleanValue anyBoolean() {
        return booleanRegexp(".*");
    } // TBD - write a better regexp

    public static BooleanValue trueVal() {
        return booleanVal(true);
    }

    public static BooleanValue falseVal() {
        return booleanVal(false);
    }

    public static NilValue nilVal() {
        return new NilValue();
    }

    public static ObjectValue anySuccessMessage() {
        return successMessage(".*");
    }

    public static ObjectValue anySuccessMessage(String field) {
        return successMessage(field, ".*");
    }

    public static ObjectValue anyFailureMessage() {
        return failureMessage(".*");
    }

    public static ObjectValue anyFailureMessage(String field) {
        return failureMessage(field, ".*");
    }

    public static ObjectValue successMessage(String message) {
        return message("SUCCESS", message);
    }

    public static ObjectValue successMessage(String field, String message) {
        return message("SUCCESS", field, message);
    }

    public static ObjectValue failureMessage(String message) {
        return message("FAILURE", message);
    }

    public static ObjectValue failureMessage(String field, String message) {
        return message("FAILURE", field, message);
    }

    public static ObjectValue message(String severity, String message) {
        return objectVal()
                .put("severity", stringVal(severity))
                .put("message", stringRegexp(message));
    }

    public static ObjectValue message(String severity, String field, String message) {
        return objectVal()
                .put("severity", severity)
                .put("field", field)
                .put("message", stringRegexp(message));
    }

    public static ObjectValue rootResource(String rel) {
        return objectVal().put("rel", rel).put("uri", anyString());
    }

    public static ObjectValue resource(String rel, String title, String uri) {
        return resource(rel, uri).put("title", stringVal(title));
    }

    public static ObjectValue resource(String rel, String uri) {
        return objectVal()
                .put("rel", rel)
                .put("uri", stringRegexp(".*/" + uri));
    }

    public static String print(Value value) {
        IndentingStringBuffer sb = new IndentingStringBuffer();
        value.print(sb);
        return sb.toString();
    }

    public static ResponseVerifier verifier(Environment environment, Response response) {
        return new ResponseVerifier(environment, response);
    }

    public static ResponseVerifier verify(ResponseVerifier verifier, ObjectValue body, int... statuses) throws Exception {
        return verifier.status(statuses).body(body);
    }

    public static ResponseVerifier verify(ResponseVerifier verifier, StringValue body, int... statuses) throws Exception {
        return verifier.status(statuses).body(body);
    }

    public static ResponseVerifier verify(ResponseVerifier verifier, int... statuses) throws Exception {
        return verifier.status(statuses);
    }

    public static ObjectValue toObjectVal(JSONObject j) throws Exception {
        return (ObjectValue) toValue(j);
    }

    public static ArrayValue toArrayVal(JSONArray j) throws Exception {
        return (ArrayValue) toValue(j);
    }

    private static Value toValue(Object j) throws Exception {
        if (j instanceof String) {
            return stringVal().value((String) j);
        }
        if (j instanceof Long) {
            return longVal().value((Long) j);
        }
        if (j instanceof Integer) {
            return intVal().value((Integer) j);
        }
        if (j instanceof Double) {
            return doubleVal().value((Double) j);
        }
        if (j instanceof Boolean) {
            return booleanVal().value((Boolean) j);
        }
        if (JSONObject.NULL == j) {
            return nilVal();
        }

        if (j instanceof JSONObject) {
            JSONObject jo = (JSONObject) j;
            ObjectValue ov = objectVal();
            for (Iterator<String> i = jo.keys(); i.hasNext();) {
                String key = i.next();
                ov.put(key, toValue(jo.get(key)));
            }
            return ov;
        }

        if (j instanceof JSONArray) {
            JSONArray ja = (JSONArray) j;
            ArrayValue av = arrayVal();
            for (int i = 0; i < ja.length(); i++) {
                av.add(toValue(ja.get(i)));
            }
            return av;
        }

        throw new IllegalArgumentException("Cannot convert " + j + " to a Value");
    }
}
