<%--

    Axelor Business Solutions

    Copyright (C) 2021 Axelor (<http://axelor.com>).

    This program is free software: you can redistribute it and/or  modify
    it under the terms of the GNU Affero General Public License, version 3,
    as published by the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="com.axelor.auth.AuthUtils" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.inject.Inject" %>
<%@ page import="com.axelor.apps.account.db.Notification" %>
<%@ page import="java.math.BigDecimal" %>
<%-- <%@ page import="com.axelor.dms.db.NotificationUser" %> --%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" session="true" %>
<%
    String language = AuthUtils.getUser().getLanguage();
%>
<li id="docBtn" class="nav-link-user">
    <a id="docLink" href="https://docs.axelor.com/abs/5.0/functional/" target="_blank"><i class="fa fa-book"></i></a>
</li>
<ul class="nav nav-shortcuts pull-right">
    <li class="dropdown">
        <a href="javascript:" class="dropdown-toggle" data-toggle="dropdown">
            <i class="fa fa-bell"></i>
        </a>
        <ul class="dropdown-menu my_ul" style="width: 240px !important;" >
            <%

                    out.println("<li style='padding:5px'>Aucune notification</li>");


            %>
        </ul>
    </li>
</ul>


<li id="docSplit" class="divider-vertical"></li>
<script>
    const prnBtn = document.getElementsByClassName('nav-link-user').item(0).parentNode;
    const docBtn = document.getElementById('docBtn');
    const docSplit = document.getElementById('docSplit');
    const docLink = document.getElementById('docLink');

    prnBtn.parentNode.insertBefore(docBtn, prnBtn);
    prnBtn.parentNode.insertBefore(docSplit, prnBtn);

    if ('<%= language %>' === 'fr') {
        docLink.setAttribute("href", "https://docs.axelor.com/abs/5.0-fr/functional/index.html");
    }
  for (i = 0;i<10;i++){
       prnBtn.parentNode.children[1].remove();
   }

   const pmyBtn = document.getElementsByClassName("divider-vertical");

</script>