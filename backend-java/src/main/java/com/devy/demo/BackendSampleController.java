package com.devy.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.devy.megafence.WebGate;

/*
* ==============================================================================================
* 메가펜스 유량제어서비스 SAMPLE(JAVA) V.24.1.911
* 이 샘플소스는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 해당 내용은 공급처에 통보 바랍니다.
* 허가된 고객 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
* 작성자 : ysd@devy.co.kr
* ----------------------------------------------------------------------------------------------
*
* 대문페이지 및 상품권구매 페이지용 Backend 호출코드 SAMPLE입니다.
* BEGIN OF ~ END OF 까지 복사하여 대상 페이지에 적용해 주세요. 
* 
* <주의 사항>
*   ⊙ 공통모듈(WebGate.java)을 사용하므로 함께 배포해 주세요. 
*   ⊙ JAVA Framework을 사용하는 경우 반드시 JAVA OR JSP 중에 하나에만 적용합니다. (중복적용 금지!) 
* ==============================================================================================-->
*/

@Controller
public class BackendSampleController {
	private Logger log = LoggerFactory.getLogger(BackendSampleController.class);
	
	
	/*
	 * 대문페이지용 Controller sample
	 */
	@GetMapping({"/", "/main.do"}) 
    public String main(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	
    	/* BEGIN OF 유량제어 코드삽입
    	 * ==============================================================
    	 * */
    	//log.info("[STEP-0] 유량제어 체크 시작");
    	String serviceId 	= "7070"; 	// 할당된 SERVICE ID 
    	String gateId 		= "26490";  // 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	
    	WebGate webgate = new WebGate();
    	// 대기표 검증하여 유효하지 않으면 대기UI 화면 컨텐츠로 응답 교체
    	if(false == webgate.WG_IsValidToken(serviceId, gateId, request, response))
    	{
       	 	/*
       	 	 * ★TO-DO 
       	 	 * 1) Framework에 따라 아래의 redirect 처리(intro.html로 redirect)가 오류날 수 있으므로 잘 동작하도록 확인 바랍니다!
       	 	 * 2) 운영서버 최종 배포 시에는 stage값을 "PRD"로 변경해 주세요.  
       	 	 */
    		String stage = "DEV"; // "DEV" OR "PRD"
    		if(stage.equals("DEV")) // 개발
    			return "redirect:/intro.html";
    		else // 운영
    			return "redirect:https://cdn.pintopia.co.kr/intro.html";
    	} 
    	/* ==============================================================
    	 * END OF 유량제어 코드삽입 
    	 * */
    	
    	
    	
    	/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
    	 * 여기까지 왔다면 유량제어 체크가 완료된 상태입니다. 
    	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    	/*	========================================================================
    		Heavy business logic ....
    		========================================================================
    		예) 고객등급 GET : 고객 DB에서 고객등급(Bronze/Silver/Gold) 조회 
    		예) 주문상태 GET : 주문 DB에서 주문상태(주문완료/배송중/배송완료)별 수량 조회
    	*/
    	
    	return "main.do";     
    }
    	
	
	/*
	 * 구매하기 페이지용 Controller sample
	 */
	@GetMapping({"/purchase/couponSelect.do" }) 
    public String couponSelect(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	
    	/* BEGIN OF 유량제어 코드삽입 : main과 동일 코드임
    	 * ==============================================================
    	 * */
    	//log.info("[STEP-0] 유량제어 체크 시작");
    	String serviceId 	= "7070"; 	// 할당된 SERVICE ID 
    	String gateId 		= "26490";  // 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	
    	WebGate webgate = new WebGate();
    	// 대기표 검증하여 유효하지 않으면 대기UI 화면 컨텐츠로 응답 교체
    	if(false == webgate.WG_IsValidToken(serviceId, gateId, request, response))
    	{
       	 	/*
       	 	 * ★TO-DO 
       	 	 * 1) Framework에 따라 아래의 redirect 처리(intro.html로 redirect)가 오류날 수 있으므로 잘 동작하도록 확인 바랍니다!
       	 	 * 2) 운영서버 최종 배포 시에는 stage값을 "PRD"로 변경해 주세요.  
       	 	 */
    		String stage = "DEV"; // "DEV" OR "PRD"
    		if(stage.equals("DEV")) // 개발
    			return "redirect:/intro.html";
    		else // 운영
    			return "redirect:https://cdn.pintopia.co.kr/intro.html";
    	} 
    	/* ==============================================================
    	 * END OF 유량제어 코드삽입 
    	 * */
    	
    	
    	
    	/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
    	 * 여기까지 왔다면 유량제어 체크가 완료된 상태입니다. 
    	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    	/*	========================================================================
    		Heavy business logic ....
    		========================================================================
    		예) 고객등급 GET : 고객 DB에서 고객등급(Bronze/Silver/Gold) 조회 
    		예) 주문상태 GET : 주문 DB에서 주문상태(주문완료/배송중/배송완료)별 수량 조회
    	*/
    	
    	return "couponSelect.do";     
    }


}

