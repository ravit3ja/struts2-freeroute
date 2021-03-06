package com.bastengao.struts2.freeroute;

import com.bastengao.struts2.freeroute.annotation.ContentBase;
import com.bastengao.struts2.freeroute.annotation.CookieValue;
import com.bastengao.struts2.freeroute.annotation.MethodType;
import com.bastengao.struts2.freeroute.annotation.Route;
import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 路由映射
 * <p/>
 * 此类为不变类(一但构造后，不会改变)
 *
 * @author bastengao
 * @date 12-12-16 20:17
 * @since 1.0
 */
class RouteMapping {
    //可选配置(可能有，可能没有)
    private final ContentBase contentBase;
    // 方法上的 @Route (methodRoute).
    private final Route route;
    // route path. 路由路径
    private final String routePath;
    // http methods.  unmodified
    private final List<MethodType> httpMethods;
    // http params.  unmodified
    private final List<String> httpParams;
    // struts interceptors. unmodified
    private final List<String> interceptors;

    // http 参数规则. unmodified
    private List<Param> params;
    //controller
    private final Class action;
    //被 @Route 注解的方法
    private final Method method;

    // routePath 上是否有 pathVariable
    private boolean hasPathVariables;
    //路径上的变量名. unmodified
    private List<String> variableNames;
    //匹配请求是否适合此路由的正则
    private Pattern routePathPattern;


    // 被 @CookieValue 注解的 field. unmodified
    private Map<CookieValue, Field> cookieValues;


    // 缓存 actionInfo
    private ActionInfo actionInfo;


    public RouteMapping(Route route, Class action, Method method) {
        this(null, null, route, action, method);
    }

    public RouteMapping(ContentBase contentBase, Route route, Class action, Method method) {
        this(contentBase, null, route, action, method);
    }

    public RouteMapping(Route controllerRoute, Route methodRoute, Class action, Method method) {
        this(null, controllerRoute, methodRoute, action, method);
    }

    public RouteMapping(ContentBase contentBase, Route controllerRoute, Route methodRoute, Class action, Method method) {

        this.contentBase = contentBase;
        this.route = methodRoute;
        this.action = action;
        this.method = method;

        this.routePath = parseRoutePath(controllerRoute, methodRoute);
        this.httpMethods = Collections.unmodifiableList(Arrays.asList(route.method()));
        this.httpParams = Collections.unmodifiableList(Arrays.asList(route.params()));
        this.interceptors = Collections.unmodifiableList(Arrays.asList(route.interceptors()));

        initParams();
        initPathVariables();
        initCookieValues();
        initActionInfo();
    }

    private String parseRoutePath(Route controllerRoute, Route methodRoute) {
        // 如果 controllerRoute 为空直接返回 methodRoute.value
        if (controllerRoute == null) {
            return methodRoute.value();
        }

        // 如果 methodRoute.value 为空直接返回 controllerRoute.value
        if (Strings.isNullOrEmpty(methodRoute.value())) {
            return controllerRoute.value();
        }

        // 去掉controllerRoute 的尾, 加上 methodRoute 的头
        return ActionUtil.shrinkEndSlash(controllerRoute.value()) + ActionUtil.padSlash(methodRoute.value());
    }

    /**
     * 初始化 params
     */
    private void initParams() {
        ArrayList<Param> params = new ArrayList<Param>();
        for (String param : route.params()) {
            params.add(new Param(param));
        }
        params.trimToSize(); // 减少空间
        this.params = Collections.unmodifiableList(params);
    }

    /**
     * 初始化 pathVariables
     */
    private void initPathVariables() {
        this.hasPathVariables = RouteUtil.hasPathVariables(routePath);
        if (hasPathVariables) {
            routePathPattern = Pattern.compile(RouteUtil.toRoutePathPattern(routePath));
            variableNames = Collections.unmodifiableList(RouteUtil.pathVariableNames(routePath));
        }
    }

    /**
     * 初始化 cookie
     */
    private void initCookieValues() {
        cookieValues = new HashMap<CookieValue, Field>();
        for (Field field : action.getDeclaredFields()) {
            if (ReflectUtil.isAnnotationPresentOfField(field, CookieValue.class)) {
                CookieValue cookieValue = ReflectUtil.getAnnotationOfField(field, CookieValue.class);
                cookieValues.put(cookieValue, field);
            }
        }
        cookieValues = Collections.unmodifiableMap(cookieValues);
    }

    /**
     * 缓存 actionInfo
     */
    private void initActionInfo() {
        String routePath = ActionUtil.padSlash(this.routePath);
        routePath = RouteUtil.flatRoutePath(routePath);

        String namespace = ActionUtil.namespace(routePath);
        String actionName = ActionUtil.actionName(routePath);

        String methodName = this.method.getName();
        String className = this.action.getName();

        // TODO 优化：为了避免名字冲突，目前简单粗暴的使用 方法名 + 类名 的方式命名
        // 缺点就是 actionName 太长，可能会影响性能
        actionName = actionName + "#" + methodName + "@" + className;
        this.actionInfo = new ActionInfo(namespace, actionName);
    }

    public ActionInfo toAction() {
        return this.actionInfo;
    }

    public ContentBase getContentBase() {
        return contentBase;
    }

    public String getRoutePath() {
        return routePath;
    }

    /**
     * unmodified list
     *
     * @return
     */
    public List<String> getHttpParams() {
        return httpParams;
    }

    /**
     * unmodified list
     *
     * @return
     */
    public List<MethodType> getHttpMethods() {
        return httpMethods;
    }

    /**
     * unmodified list
     *
     * @return
     * @since 1.0.2
     */
    public List<String> getInterceptors() {
        return interceptors;
    }

    public List<Param> getParams() {
        return params;
    }

    public Class getAction() {
        return action;
    }

    public Method getMethod() {
        return method;
    }

    public boolean hasPathVariables() {
        return hasPathVariables;
    }

    public List<String> getVariableNames() {
        return variableNames;
    }

    public Pattern getRoutePathPattern() {
        return routePathPattern;
    }

    public Map<CookieValue, Field> getCookieValues() {
        return cookieValues;
    }

    public String prettyPath() {
        return prettyMethods(route.method()) + " " +
                routePath +
                prettyParams(route.params());
    }

    /**
     * 打印 @Route.params
     *
     * @param params
     * @return
     */
    public static String prettyParams(String[] params) {
        if (params == null) {
            return "";
        }

        if (params.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("?");
        boolean isFirst = true;
        for (String param : params) {
            if (!isFirst) {
                sb.append("&");
            }
            sb.append(param);
            if (isFirst) {
                isFirst = !isFirst;
            }
        }
        return sb.toString();
    }


    /**
     * 打印 @Route.method.
     *
     * @param types
     * @return
     */
    public static String prettyMethods(MethodType[] types) {
        if (types == null || types.length == 0) {
            return "NONE";
        }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (MethodType type : types) {
            if (!isFirst) {
                sb.append(" | ");
            }
            sb.append(type.toString());

            if (isFirst) {
                isFirst = !isFirst;
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "RouteMapping{" +
                "route=" + prettyPath() +
                ", action=" + action +
                ", method=" + method.getName() +
                '}';
    }

    /**
     * 表示一个 http param 表达式
     */
    public static class Param {
        private String paramName;

        /**
         * 是否需要存在性检查
         */
        private boolean needForExistingCheck = false;

        /**
         * 是否需要表达式检查(等于或者不等于)
         */
        private boolean needForEqualExpressionCheck = false;
        /**
         * 相等或者不相等
         */
        private boolean equalOrNot = true;
        /**
         * 比较的值
         */
        private String paramValue;

        public Param(String param) {
            //TODO 分析表达示这还不够严谨
            //是否有表达示
            if (param.contains("=")) {
                //需要表达式检查
                this.needForEqualExpressionCheck = true;
                //不相等表达示
                if (param.contains("!=")) {
                    //不相等
                    equalOrNot = false;
                    String[] paramPair = param.split("\\!\\=");
                    paramName = paramPair[0];
                    paramValue = paramPair[1];
                }
                //相等表达示
                else if (param.contains("=")) {
                    //相等
                    equalOrNot = true;
                    String[] paramPair = param.split("=");
                    paramName = paramPair[0];
                    paramValue = paramPair[1];
                }
            } else {
                this.needForExistingCheck = true;
                this.paramName = param;
            }
        }

        /**
         * 查看当前请求是否匹配此 param
         *
         * @param request
         * @param params
         * @return
         */
        public boolean match(HttpServletRequest request, Set<String> params) {
            if (needForExistingCheck) {
                return params.contains(paramName);
            }

            if (needForEqualExpressionCheck) {
                if (params.contains(paramName)) {
                    String value = request.getParameter(paramName);
                    if (!Strings.isNullOrEmpty(value)) {
                        return equalOrNot == (paramValue.equals(value));
                    }
                }
            }

            return false;
        }

    }
}
