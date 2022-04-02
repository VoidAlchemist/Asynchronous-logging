package com.xenon.utils.readability;

import jdk.jfr.ContentType;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Label;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the possible values taken by a variable.
 * @author Zenon
 */
@SuppressWarnings("unused")
@ContentType
@Label("Possible Values")
@Description("Different possible values for an object")
@DataAmount
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER})
public @interface Values {
    String[] value();
}
