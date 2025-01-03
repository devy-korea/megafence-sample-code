<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--
################################################################################################# 
Controller (IndexController.java)에 유량제어 서비스를 적용했으므로, 
이 페이지(JSP)에는 유량제어 관련 어떤 작업도 필요하지 않습니다!!!
원래의 업무페이지를 그대로 사용합니다.
################################################################################################# 
-->

<!DOCTYPE html>
<html>
<head>
    <!-- custom css -->
    <link href="https://dist.devy.kr/bulma-0.7.1/bulma.css" rel="stylesheet" />
</head>
<body>
    <div id="app" class="container">
        <form id="form1">
            <div class="notification is-info">
                <h1 class="title">BACKEND : LANDING 방식</h1>
                <h2 class="has-text-light">페이지 진입 시 Backend 호출코드가 동작하여 대기페이지(landing.jsp)를 거쳐 왔습니다.</h2>
                <h2 class="has-text-light">새로 고침을 5~10회 반복하면 다시 대기페이지를 다녀 옵니다. </h2>
                
                <hr/>
                <a href="/index">HOME</a>
            </div>
        </form>
    </div>
    
    
</body>
