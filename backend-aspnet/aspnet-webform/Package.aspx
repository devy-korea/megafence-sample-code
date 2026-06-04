<%@ Page Title="패키지 소개" Language="C#" MasterPageFile="~/Site.master" AutoEventWireup="true" CodeBehind="Package.aspx.cs" Inherits="AspNetWebForm.Package" %>

<asp:Content ContentPlaceHolderID="MainContent" runat="server">
    <section class="wrap">
        <span class="badge">PACKAGE</span>
        <h1>달나라 로켓여행 패키지</h1>
        <p class="lead">7일 6박(우주 3박 / 발사기지 3박)의 프리미엄 우주여행 풀패키지.</p>

        <div class="grid grid-2" style="margin-top:24px;">
            <div class="card">
                <h2>패키지 구성</h2>
                <ul class="check">
                    <li>LUNA-X 유인 우주선 왕복 탑승권 (1인 1좌석)</li>
                    <li>달 궤도 우주호텔 'Selene' 3박 숙박</li>
                    <li>월면 착륙 및 트레킹 1회 (우주비행사 동행)</li>
                    <li>맞춤 제작 우주복 및 기념 패치 증정</li>
                    <li>발사 전 6주 우주 적응 훈련 캠프</li>
                    <li>전 일정 의료진·보험 포함</li>
                </ul>
            </div>
            <div class="card">
                <h2>가격</h2>
                <div class="price">₩ 1,200,000,000</div>
                <p class="muted" style="margin:8px 0 18px;">1인 기준 · 부가세 포함 · 선착순 100석 한정</p>
                <table class="tbl">
                    <tr><th>예약금</th><td>₩ 120,000,000 (10%)</td></tr>
                    <tr><th>잔금</th><td>출발 60일 전 완납</td></tr>
                    <tr><th>할부</th><td>최대 12개월 무이자</td></tr>
                </table>
                <div style="margin-top:20px;"><a class="btn btn-lg" href="Purchase.aspx">예약하기</a></div>
            </div>
        </div>
    </section>
</asp:Content>
