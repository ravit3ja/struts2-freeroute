package com.gaohui.struts2.mapping;

import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bastengao
 * @date 12-12-16 00:11
 */
public class DefaultActionMapper extends org.apache.struts2.dispatcher.mapper.DefaultActionMapper {
    private static final Logger log = LoggerFactory.getLogger(DefaultActionMapper.class);

    private RouteMappingHandler routeMappingHandler;

    @Inject("routeMappingHandler")
    public void setRouteMappingHandler(RouteMappingHandler routeMappingHandler) {
        log.trace("routeMappingHandler:{}", routeMappingHandler);
        this.routeMappingHandler = routeMappingHandler;
    }

    @Inject(required = false, value = "struts.mapping.myConfig")
    public void setMyConfig(String config) {
        log.debug("config:{}", config);
    }

    @Override
    public ActionMapping getMapping(javax.servlet.http.HttpServletRequest request, ConfigurationManager configManager) {
        log.debug("mapper:{}", this);
        log.debug("getMapping:{}", request);
        log.debug("uri:{}", request.getRequestURI());
        log.debug("url:{}", request.getRequestURL());
        log.debug("servletPath:{}", request.getServletPath());


        ActionMapping actionMapping0 = parseAndFindRouteMapping(request);
        if (true) {
            if (actionMapping0 != null) {
                return actionMapping0;
            }
        }


        ActionMapping actionMapping = super.getMapping(request, configManager);

        if (actionMapping != null) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("id", 999);
            actionMapping.setParams(params);
        }
        return actionMapping;
    }

    private ActionMapping parseAndFindRouteMapping(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        RouteMapping routeMapping = routeMappingHandler.route(servletPath);

        if (routeMapping == null) {
            return null;
        }
        log.debug("routeMapping:{}", routeMapping);

        String routePath = routeMapping.getRoute().value();
        routePath = ActionUtil.padSlash(routePath);

        String namespace = ActionUtil.namespace(routePath);
        namespace = ActionUtil.padSlash(namespace);
        String actionName = ActionUtil.actionName(routePath);

        ActionMapping actionMapping = new ActionMapping();
        actionMapping.setNamespace(namespace);
        actionMapping.setName(actionName);
        return actionMapping;
    }
}
