/*
 * ConditionalSetTag.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.jsp.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.common.core.SetSupport;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * Like the JSTL core set tag, but with support for an
 * internal condition.  Useful for cases where there are
 * only two options and using the c:choose tag is overly
 * cumbersome.
 *
 * Old way:
 * <pre>
 *      <c:choose>
 *          <c:when test="${not empty something}">
 *              <c:set var="var" value="0" />
 *          </c:when>
 *          <c:otherwise>
 *              <c:set var="var" value="1" />
 *          </c:otherwise>
 *      </c:choose>
 * </pre>
 *
 * New way:
 * <pre>
 *      <standard:cset test="${not empty something}" ifTrue="0" ifFalse="1" />
 * </pre>
 *
 * @author Ryan Wilson
 *
 * Compiled from {@link org.apache.taglibs.standard.tag.el.core.SetTag} and
 * {@link org.apache.taglibs.standard.tag.el.core.SetTag}
 */
public class ConditionalSetTag extends SetSupport {

    //*********************************************************************
    // Constructor and lifecycle management

    // initialize inherited and local state
    /**
     * 
     */
    public ConditionalSetTag()
    {
        super();
        init();
    }


    //*********************************************************************
    // Private state

    private String test;               // the value of the 'test' attribute

    private String valIfTrue_;
    private String valIfFalse_;
    
    private String target_;
    private String property_;


    //*********************************************************************
    // Tag logic

    /**
     *
     * @return
     * @throws JspException
     */
    @Override
    public int doStartTag() throws JspException {

        // evaluate any expressions we were passed, once per invocation
        evaluateExpressions();

        return super.doStartTag();
    }

    // Releases any resources we may have (or inherit)
    /**
     *
     */
    @Override
    public void release() {
        super.release();
        init();
    }


    //*********************************************************************
    // Accessor methods

    /**
     * 
     * @param valueT
     */
    public void setIfTrue(String valueT)
    {
        this.valIfTrue_ = valueT;
        this.valueSpecified = true;
    }

    /**
     * 
     * @param valueF
     */
    public void setIfFalse(String valueF)
    {
        this.valIfFalse_ = valueF;
        this.valueSpecified = true;
    }

    /**
     * 
     * @param target_
     */
    public void setTarget(String target_)
    {
        this.target_ = target_;
    }

    /**
     * 
     * @param property_
     */
    public void setProperty(String property_)
    {
        this.property_ = property_;
    }

    /**
     * 
     * @param test
     */
    public void setTest(String test)
    {
        this.test = test;
    }

    //*********************************************************************
    // Private Utility functions
    
    private void init() {
        // null implies "no expression"
        test = valIfTrue_ = valIfFalse_ = target_ = property_ = null;
    }

    // Supplied conditional logic
    /**
     * 
     * @return
     * @throws JspTagException
     */
    protected boolean condition() throws JspTagException
    {
	try {
            Object r = ExpressionEvaluatorManager.evaluate("test", test, Boolean.class, this, pageContext);
            if (r == null)
                throw new NullAttributeException("if", "test");
	    else
	        return (((Boolean) r).booleanValue());
        } catch (JspException ex) {
	    throw new JspTagException(ex.toString(), ex);
	}
    }

    // Evaluates expressions as necessary
    private void evaluateExpressions() throws JspException {
        /*
         * Note: we don't check for type mismatches here; we assume
         * the expression evaluator will return the expected type
         * (by virtue of knowledge we give it about what that type is).
         * A ClassCastException here is truly unexpected, so we let it
         * propagate up.
         */

	// 'value'
	try {
	    value = ExpressionUtil.evalNotNull("set", "value", condition() ? valIfTrue_ : valIfFalse_, Object.class, this, pageContext);
	} catch (NullAttributeException ex) {
	    // explicitly let 'value' be null
	    value = null;
	}

	// 'target'
	target = ExpressionUtil.evalNotNull("set", "target", target_, Object.class, this, pageContext);

	// 'property'
	try {
	    property = (String) ExpressionUtil.evalNotNull("set", "property", property_, String.class, this, pageContext);
        } catch (NullAttributeException ex) {
            // explicitly let 'property' be null
            property = null;
        }
    }
}
