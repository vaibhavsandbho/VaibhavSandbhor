package com.ats.lumax.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpcUaValueConverter {

    public Object convertValue(Variant variant) {
        if (variant == null || variant.getValue() == null) {
            return null;
        }

        Object value = variant.getValue();

        // Handle byte arrays (ASCII)
        if (value instanceof byte[]) {
            try {
                // Convert byte array to string, trimming null characters
                String str = new String((byte[]) value, "UTF-8").trim();
                // Replace ASCII 32 (space) with empty string
                if (str.equals("32")) {
                    return "";
                }
                // If the string contains only printable characters, return it
                if (str.matches("\\A\\p{Print}+\\z")) {
                    return str;
                }
            } catch (Exception e) {
                log.debug("Could not convert byte array to string", e);
            }
        }

        // Handle arrays of UByte or other numeric types
        if (value instanceof UByte[]) {
            try {
                byte[] bytes = new byte[((UByte[]) value).length];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = ((UByte[]) value)[i].byteValue();
                }
                String str = new String(bytes, "UTF-8").trim();
                // Replace ASCII 32 (space) with empty string
                if (str.equals("32")) {
                    return "";
                }
                if (str.matches("\\A\\p{Print}+\\z")) {
                    return str;
                }
            } catch (Exception e) {
                log.debug("Could not convert UByte array to string", e);
            }
        }

        // Handle single numeric value of 32
        if (value instanceof Number && ((Number)value).intValue() == 32) {
            return "";
        }

        // Return original value if no conversion was needed or possible
        return value;
    }

    public DataValue convertDataValue(DataValue originalValue) {
        if (originalValue == null || originalValue.getValue() == null) {
            return originalValue;
        }

        Object convertedValue = convertValue(originalValue.getValue());
        return new DataValue(
            new Variant(convertedValue),
            originalValue.getStatusCode(),
            originalValue.getSourceTime(),
            originalValue.getServerTime()
        );
    }

    public Object convertVariantToJavaType(Variant variant) {
        if (variant == null || variant.getValue() == null) {
            return null;
        }

        Object value = variant.getValue();
        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Float) {
                return (Float) value;
            } else if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Short) {
                return (Short) value;
            } else if (value instanceof Byte) {
                return (Byte) value;
            } else if (value instanceof String) {
                return (String) value;
            } else {
                log.warn("Unsupported value type: {}", value.getClass().getSimpleName());
                return value.toString();
            }
        } catch (Exception e) {
            log.error("Error converting variant value: {}", value, e);
            return null;
        }
    }

    public Variant convertJavaTypeToVariant(Object value) {
        if (value == null) {
            return new Variant(null);
        }

        try {
            if (value instanceof Boolean) {
                return new Variant((Boolean) value);
            } else if (value instanceof Integer) {
                return new Variant((Integer) value);
            } else if (value instanceof Long) {
                return new Variant((Long) value);
            } else if (value instanceof Float) {
                return new Variant((Float) value);
            } else if (value instanceof Double) {
                return new Variant((Double) value);
            } else if (value instanceof Short) {
                return new Variant((Short) value);
            } else if (value instanceof Byte) {
                return new Variant((Byte) value);
            } else if (value instanceof String) {
                return new Variant((String) value);
            } else {
                log.warn("Unsupported value type: {}", value.getClass().getSimpleName());
                return new Variant(value.toString());
            }
        } catch (Exception e) {
            log.error("Error converting value to variant: {}", value, e);
            return new Variant(null);
        }
    }
} 