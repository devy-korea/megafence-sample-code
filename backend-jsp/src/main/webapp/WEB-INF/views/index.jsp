<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!-- 
* ==============================================================================================
* 메가펜스 유량제어서비스 SAMPLE(JSP) V.21.1.4
* 이 샘플소스는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 해당 내용은 공급처에 통보 바랍니다.
* 허가된 고객 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
* 작성자 : ysd@devy.co.kr
* ----------------------------------------------------------------------------------------------
*
* BACKEND 방식 : 페이지 로드 시 Backend 코드(java,jsp,php 등..)로 유량제어 서비스 호출
*
* <이용 안내> 
*   ⊙ 아래의 샘플코드를 그대로 테스트용 페이지에 삽입해서 대기UI가 표시되는지 확인
*   ⊙ 서비스 세팅이 완료되면 안내받은 GATE_ID, SERVICE_ID로 수정해서 사용
*   ⊙ 유량제어 코드는 DB접속 등의 부하량이 많은 업무로직 이전에 삽입해야 효과적입니다.
*   ⊙ 쿠키나 세션 등을 이용하는 간단한 처리는 유량제어 코드 이전에 배치되어도 무방합니다.
*
* <주의 사항>
*   ⊙ 공통모듈(webgate-lib.jsp)을 사용하므로 함께 배포해 주세요. 
*   ⊙ JAVA Framework을 사용하는 경우 반드시 JAVA OR JSP 중에 하나에만 적용합니다. (중복적용 금지!) 
* ==============================================================================================-->

<!--
Light business logic ....
	예) 로그인 체크 : 쿠키나 세션을 체크해서 Login 페이지로 redirect
-->


<!--
BEGIN OF 유량제어 코드삽입
▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼--> 
<%@ include file="./webgate-lib.jsp" %>
<%
	// Setting 
	String serviceId 	= "9000"; // 할당된 SERVICE ID
	String gateId 		= "1"; 	  // 사용할 GATE ID (할당된 GATE ID 중에서 사용) 

	// 유량제어 체크 : 접속자가 많으면 대기UI로 응답 대체
	if(WG_IsNeedToWaiting(serviceId, gateId,  request, response))
  	{
	  	out.print(WG_GetWaitingUi(serviceId, gateId));
	  	return; // 응답종료
  	}
%>
<!--
▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ 
END OF 유량제어 코드삽입 -->


<!--
Heavy business logic ....
	예) 고객등급 GET : 고객 DB에서 고객등급(Bronze/Silver/Gold) 조회 
	예) 주문상태 GET : 주문 DB에서 주문상태(주문완료/배송중/배송완료)별 수량 조회
-->



<!DOCTYPE html>
<html>
<head>
    <!-- custom css -->
    <link href="//cdn.devy.kr/dist/bulma-0.7.1/bulma.css" rel="stylesheet" />
</head>
<body>
    <div id="app" class="container">
        <form id="form1">
            <div class="notification is-info">
                <h1 class="title">Hello, World!!!</h1>
                <h2 class="has-text-light">Backend 방식의 유량제어를 적용한 SAMPLE 업무페이지.</h2>
                <h2 class="has-text-light">대문(index), 이벤트 안내, 상품상세, 주문하기 페이지 등 Backend 코드(JAVA/JSP/PHP/ASP.NET) 삽입이 가능한 모든 페이지에 적용할 수 있습니다.</h2>
            </div>

            <hr/>
            <div class="notification is-white has-text-centered">
                <a class="button is-dark" href="frontend-sample.html">Frontend 방식 DEMO</a> <a class="button is-dark" href="landing-sample.html">Landing 방식 DEMO</a>
            </div>
        </form>
    </div>
</body>



