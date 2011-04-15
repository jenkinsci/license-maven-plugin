<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:l="licenses">

  <xsl:template match="/">
    <html>
      <body>
        <table>
          <tr>
            <th>Name</th>
            <th>Maven ID</th>
            <th>License</th>
          </tr>
          <xsl:for-each select="//l:dependency">
            <tr>
              <td>
                <a href="{@url}">
                  <xsl:value-of select="@name"/>
                </a>
              </td>
              <td>
                <xsl:value-of select="@groupId" />:<xsl:value-of select="@artifactId" />:<xsl:value-of select="@version" />
              </td>
              <td>
                <xsl:for-each select="l:license">
                  <a href="{@url}">
                    <xsl:value-of select="@name"/>
                  </a>
                  <br/>
                </xsl:for-each>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>