<#import "Header.ftl" as header>
<#import "Footer.ftl" as footer>

<!doctype html>
<!--
Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.

This file is part of essencium-backend.

essencium-backend is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

essencium-backend is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
-->
<html>
<head>
  <meta name="viewport" content="width=device-width">
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title>${subject}</title>
  <@header.head />
</head>
<body class="" style="background-color: #f6f6f6; font-family: sans-serif; -webkit-font-smoothing: antialiased; font-size: 14px; line-height: 1.4; margin: 0; padding: 0; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;">
<span class="preheader" style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">${subject}</span>
<table border="0" cellpadding="0" cellspacing="0" class="body" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; background-color: #f6f6f6;">
  <tr>
    <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">&nbsp;</td>
    <td class="container" style="font-family: sans-serif; font-size: 14px; vertical-align: top; display: block; Margin: 0 auto; max-width: 580px; padding: 10px; width: 580px;">
      <div class="content" style="box-sizing: border-box; display: block; Margin: 0 auto; max-width: 580px; padding: 10px;">

        <!-- START CENTERED WHITE CONTAINER -->
        <table class="main" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; background: #ffffff; border-radius: 3px;">

          <!-- START MAIN CONTENT AREA -->
          <tr>
            <td class="wrapper" style="font-family: sans-serif; font-size: 14px; vertical-align: top; box-sizing: border-box; padding: 20px;">
              <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%;">
                <tr>
                  <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
                    <p style="text-align:center;">
                      <a href="${mailBranding.url}" style="text-decoration: none;color:black;">
                        <img src="${mailBranding.logo}" style="max-width: 100px;">
                        <br /><br />
                        <span style="font-size: 20px;">${mailBranding.name}</span>
                      </a>
                    </p>
                    <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">Lieber Nutzer,</p>
                    <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">für Ihren Account bei ${mailBranding.name} wurde ein neues Passwort angefragt. Bitte klicken Sie auf den untenstehenden Link, um ein Passwort zu vergeben.</p>
                    <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">Wenn Sie das Zurücksetzen des Passworts nicht selbst angefordert haben, können Sie diese Mail einfach ignorieren.</p>
                    <table border="0" cellpadding="0" cellspacing="0" class="btn btn-primary" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: 100%; box-sizing: border-box;">
                      <tbody>
                      <tr>
                        <td align="left" style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
                          <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: separate; mso-table-lspace: 0pt; mso-table-rspace: 0pt; width: auto;">
                            <tbody>
                            <tr>
                              <td style="font-family: sans-serif; font-size: 14px; vertical-align: top; background-color: ${mailBranding.primaryColor}; border-radius: 5px; text-align: center;"> <a href="${resetLink}${resetToken}" target="_blank" style="display: inline-block; color: ${mailBranding.textColor}; background-color: ${mailBranding.primaryColor}; border: solid 1px ${mailBranding.primaryColor}; border-radius: 5px; box-sizing: border-box; cursor: pointer; text-decoration: none; font-size: 14px; font-weight: bold; margin: 0; padding: 6px 12px; border-color: ${mailBranding.primaryColor};">Passwort zurücksetzen</a> </td>
                            </tr>
                            </tbody>
                          </table>
                        </td>
                      </tr>
                      </tbody>
                    </table>
                    <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;">Mit freundlichen Grüßen</p>
                    <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0; margin-bottom: 15px;"><i>Ihr Team von ${mailBranding.name}</i></p>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- END MAIN CONTENT AREA -->
        </table>

        <!-- START FOOTER -->
        <@footer.footerDiv/>
        <!-- END FOOTER -->

        <!-- END CENTERED WHITE CONTAINER -->
      </div>
    </td>
    <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">&nbsp;</td>
  </tr>
</table>
</body>
</html>