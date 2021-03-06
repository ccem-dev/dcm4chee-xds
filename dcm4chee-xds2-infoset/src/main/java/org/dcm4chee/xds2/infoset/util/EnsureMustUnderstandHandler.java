/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4chee.xds2.infoset.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnsureMustUnderstandHandler implements SOAPHandler<SOAPMessageContext> {

    private List<String> mustUnderstandHeaders; 
    
    private static Logger log = LoggerFactory.getLogger(EnsureMustUnderstandHandler.class);
    
    public EnsureMustUnderstandHandler() {
        this("Action", "To", "ReplyTo");
    }
    public EnsureMustUnderstandHandler(String... headernames) {
        mustUnderstandHeaders = Arrays.asList(headernames);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        log.debug("##########EnsureMustUnderstandHandler called!");
        if (Boolean.TRUE.equals(ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))) {
            try {
                SOAPHeader soapHdr = ctx.getMessage().getSOAPHeader();
                SOAPHeaderElement hdr;
                for (Iterator<SOAPHeaderElement> iter = soapHdr.examineAllHeaderElements() ; iter.hasNext();) {
                    hdr = iter.next();
                    log.debug("##### hdr:{}",hdr.getNodeName());
                    log.debug("##### hdr.getMustUnderstand:{}",hdr.getMustUnderstand());
                    if (mustUnderstandHeaders.contains(hdr.getNodeName()))
                            hdr.setMustUnderstand(true);
                }
            } catch (SOAPException e) {
                log.warn("Failed to ensure mustUnderstand SOAP Header!", e);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}
