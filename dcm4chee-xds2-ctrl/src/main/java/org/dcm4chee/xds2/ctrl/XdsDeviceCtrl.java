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
 * Portions created by the Initial Developer are Copyright (C) 2012
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

package org.dcm4chee.xds2.ctrl;

import org.dcm4che3.conf.core.api.internal.BeanVitalizer;
import org.dcm4che3.conf.api.internal.DicomConfigurationManager;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.dcm4chee.xds2.common.cdi.Xds;
import org.dcm4chee.xds2.common.XdsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

@Path("/ctrl")
@RequestScoped
public class XdsDeviceCtrl {

    @Inject
    @Xds
    private XdsService service;

    public static final Logger log = LoggerFactory.getLogger(XdsDeviceCtrl.class);

    @Context
    private HttpServletRequest request;

    @GET
    @Path("running")
    public String isRunning() {
        return String.valueOf(service.isRunning());
    }

    @GET
    @Path("start")
    public Response start() throws Exception {
        service.start();
        return Response.status(Status.OK).build();
    }

    @GET
    @Path("stop")
    public Response stop() {
        service.stop();
        return Response.status(Status.OK).build();
    }

    @GET
    @Path("reload")
    public Response reload() throws Exception {
        service.reload();
        log.info("Service reconfigured (device {})", service.getDevice().getDeviceName());
        return Response.status(Status.NO_CONTENT).build();
    }


    @Inject
    DicomConfigurationManager configurationManager;


    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getConfig() throws Exception {

        BeanVitalizer vitalizer = configurationManager.getVitalizer();
        Map<String, Object> map = (Map<String, Object>) vitalizer.lookupDefaultTypeAdapter(Device.class).toConfigNode(
                service.getDevice(),
                null,
                configurationManager.getTypeSafeConfiguration().getContextFactory().newSavingContext());

        return map;
    }


}
