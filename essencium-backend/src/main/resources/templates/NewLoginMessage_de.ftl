<#import "Header.ftl" as header>
<#import "Footer.ftl" as footer>

<!doctype html>
<html lang="de">
<head>
    <meta name="viewport" content="width=device-width">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>${subject}</title>
    <@header.head />
</head>
<body class=""
      style="background-color: #f6f6f6; font-family: sans-serif; -webkit-font-smoothing: antialiased; font-size: 14px; line-height: 1.4; margin: 0; padding: 0; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;">
<span class="preheader"
      style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">${subject}</span>
<table  class="body"
       style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: 100%; background-color: #f6f6f6;">
    <tr>
        <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">&nbsp;</td>
        <td class="container"
            style="font-family: sans-serif; font-size: 14px; vertical-align: top; display: block; Margin: 0 auto; max-width: 580px; padding: 10px; width: 580px;">
            <div class="content"
                 style="box-sizing: border-box; display: block; Margin: 0 auto; max-width: 580px; padding: 10px;">

                <!-- START CENTERED WHITE CONTAINER -->
                <table class="main"
                       style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: 100%; background: #ffffff; border-radius: 3px;">

                    <!-- START MAIN CONTENT AREA -->
                    <tr>
                        <td class="wrapper"
                            style="font-family: sans-serif; font-size: 14px; vertical-align: top; box-sizing: border-box; padding: 20px;">
                            <table style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: 100%;">
                                <tr>
                                    <td style="font-family: sans-serif; font-size: 14px; vertical-align: top;">
                                        <p style="text-align:center;">
                                            <a href="${mailBranding.url}" style="text-decoration: none;color:black;">
                                                <img src="${mailBranding.logo}" style="max-width: 100px;" alt="Application Logo">
                                                <br/><br/>
                                                <span style="font-size: 20px;">${mailBranding.name}</span>
                                            </a>
                                        </p>
                                        <p style="font-family: sans-serif; font-size: 20px; font-weight: bold; margin: 0 0 15px;">
                                            Neuer Login registriert</p>
                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0 0 15px;">
                                            Soeben erfolgte eine Anmeldung bei der Anwendung ${mailBranding.name}:</p>
                                        <table class="btn btn-primary" style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: 100%; box-sizing: border-box;">
                                            <tbody>
                                            <tr>
                                                <td style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
                                                    Quelle:
                                                </td>
                                                <td style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
                                                    ${tokenRepresentation.userAgent}
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
                                                    Zeitpunkt:
                                                </td>
                                                <td style="font-family: sans-serif; font-size: 14px; vertical-align: top; padding-bottom: 15px;">
                                                    ${tokenRepresentation.issuedAt?datetime}
                                                </td>
                                            </tr>
                                            </tbody>
                                        </table>
                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0 0 15px;">
                                            Sollte dieser Login nicht durch Sie initiiert sein, melden Sie sich bitte
                                            unter
                                            <a href="${mailBranding.url}" style="text-decoration: none;color:black;">
                                                <span>${mailBranding.url}</span>
                                            </a> an und prüfen die aktiven Logins in Ihrem User-Profil.</p>

                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0 0 15px;">
                                            Mit freundlichen Grüßen</p>
                                        <p style="font-family: sans-serif; font-size: 14px; font-weight: normal; margin: 0 0 15px;">
                                            <i>Ihr Team von ${mailBranding.name}</i></p>
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