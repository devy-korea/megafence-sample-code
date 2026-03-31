package com.devy.megafence.interceptor;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.devy.megafence.WebGate;


@Component
public class WebGateInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

    	
        /* log 필요 시 사용 
        String uri = request.getRequestURI();
        String url = request.getRequestURL().toString();
        String query = request.getQueryString();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        String contentType = request.getContentType();
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        String dispatcherType = request.getDispatcherType().name();

        System.out.println("==================================================");
        System.out.println("[WebGateInterceptor] preHandle START" );
        System.out.println("method         : " + method);
        System.out.println("uri            : " + uri);
        System.out.println("url            : " + url);
        System.out.println("queryString    : " + query);
        System.out.println("remoteAddr     : " + remoteAddr);
        System.out.println("contentType    : " + contentType);
        System.out.println("referer        : " + referer);
        System.out.println("userAgent      : " + userAgent);
        System.out.println("dispatcherType : " + dispatcherType);    	
    	*/
    	
    	/* BEGIN OF 유량제어 코드삽입 */
    	/* *********************************************************************************************************
    	 * 유량제어 적용대상 PAGE이면 해당 GATE ID SET
    	 * *********************************************************************************************************/
    	String serviceId 	= "9000"; 	// *할당된 SERVICE ID로 수정 요망
    	String gateId 		= null;  	// 사용할 GATE ID (아래에서 별도 세팅)

    	// 유량제어 적용대상 페이지인지 꼭 개별적으로 체크해서 해당 GATE ID로 수정 (기타 잡다한 리소스 요청들에 의해 동작하지 않도록 주의!) 
    	String uri = request.getRequestURI();
    	switch(uri) {
    		case "/" : 
    		case "/Samples/BackendWithReplace": // *실제 경로로 수정 요망
    			gateId = "1"; 					// *실제 사용할 GATE ID로 수정 요망
    			break;
    		default : 
    			gateId = null;
    			break;
    	}
    	
    	/* *********************************************************************************************************
    	 * 유량제어 적용대상 PAGE이면 호출코드 동작 (대기표 검증하여 유효하지 않으면 대기UI 화면 컨텐츠로 응답 교체)
    	 * *********************************************************************************************************/
    	WebGate webgate = new WebGate();
    	if(gateId != null && gateId.length() > 0)
    	{
        	if(webgate.WG_IsNeedToWaiting(serviceId, gateId, request, response))
        	{
        		try {
        			
        			/* 페이지 응답을 대기UI(uiHtml)로 교체 */
        			String uiHtml = webgate.WG_GetWaitingUi(serviceId, gateId);
        			response.setContentType("text/html");
            		PrintWriter out = response.getWriter();
        			out.write(uiHtml);
        			out.close();
        			return false; // false return 중요!
    	    	} catch (Exception e) {
    	    		// 필요시 log write..
    	    	}
    	        finally {}
        	} 
    	}
    	/* END OF 유량제어 코드삽입 */
    	
    	
    	
    	
    	// 여기서 부터는 고객의 기존  업무로직 코드가 시작됩니다. 
    	// ...
    	// ...

        return true;
    }
}
