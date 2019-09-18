package com.webank.wedatasphere.linkis.ujes.jdbc

import java.sql.{Connection, Driver, DriverManager, DriverPropertyInfo, SQLFeatureNotSupportedException}
import java.util.Properties
import java.util.logging.Logger

import UJESSQLDriverMain._
import org.apache.commons.lang.StringUtils

import scala.collection.JavaConversions

/**
  * Created by enjoyyin on 2019/5/27.
  */
class UJESSQLDriverMain extends Driver {
  override def connect(url: String, info: Properties): Connection = if(acceptsURL(url)) {
    val props = if(info != null) info else new Properties
    props.putAll(parseURL(url))
    val ujesClient = UJESClientFactory.getUJESClient(props)
    new UJESSQLConnection(ujesClient, props)
  } else throw new UJESSQLException(UJESSQLErrorCode.BAD_URL, "bad url: " + url)

  override def acceptsURL(url: String): Boolean = url.startsWith(URL_PREFIX)

  private def parseURL(url: String): Properties = {
    val props = new Properties
    //add an entry to get url
    props.setProperty("URL", url)
    url match {
      case URL_REGEX(host, port, db, params) =>
        if(StringUtils.isNotBlank(host)) props.setProperty(HOST, host)
        if(StringUtils.isNotBlank(port)) props.setProperty(PORT, port.substring(1))
        if(StringUtils.isNotBlank(db) && db.length > 1) props.setProperty(DB_NAME, db.substring(1))
        if(StringUtils.isNotBlank(params) && params.length > 1) {
          val _params = params.substring(1)
          val kvs = _params.split(PARAM_SPLIT).map(_.split(KV_SPLIT)).filter {
            case Array(USER, value) =>
              props.setProperty(USER, value)
              false
            case Array(PASSWORD, value) =>
              props.setProperty(PASSWORD, value)
              false
            case Array(key, _) =>
              if(StringUtils.isBlank(key)) {
                throw new UJESSQLException(UJESSQLErrorCode.BAD_URL, "bad url for params: " + url)
              } else true
            case _ => throw new UJESSQLException(UJESSQLErrorCode.BAD_URL, "bad url for params: " + url)
          }
          props.setProperty(PARAMS, kvs.map(_.mkString(KV_SPLIT)).mkString(PARAM_SPLIT))
        }
      case _ => throw new UJESSQLException(UJESSQLErrorCode.BAD_URL, "bad url: " + url)
    }
    props
  }

  override def getPropertyInfo(url: String, info: Properties): Array[DriverPropertyInfo] = {
    val props = if(info != null) info else new Properties
    props.putAll(parseURL(url))
    val hostProp = new DriverPropertyInfo(HOST, props.getProperty(HOST))
    hostProp.required = true
    val portProp = new DriverPropertyInfo(PORT, props.getProperty(PORT))
    portProp.required = false
    val userProp = new DriverPropertyInfo(USER, props.getProperty(USER))
    userProp.required = true
    val passwordProp = new DriverPropertyInfo(PASSWORD, props.getProperty(PASSWORD))
    passwordProp.required = true
    val dbName = new DriverPropertyInfo(DB_NAME, props.getProperty(DB_NAME))
    dbName.required = false
    val paramProp = new DriverPropertyInfo(PARAMS, props.getProperty(PARAMS))
    paramProp.required = false
    Array(hostProp, portProp, userProp, passwordProp, dbName, paramProp)
  }

  override def getMajorVersion: Int = DEFAULT_VERSION

  override def getMinorVersion: Int = 0

  override def jdbcCompliant(): Boolean = false

  override def getParentLogger: Logger = throw new SQLFeatureNotSupportedException("Method not supported")
}

/**
  * modifed by owenxu 2019/8/28
  * make all variables refer to its correspondence in
  * UJESSQLDriver in order to make them consistent
  */
object UJESSQLDriverMain {
  DriverManager.registerDriver(new UJESSQLDriverMain)
  private val URL_PREFIX = UJESSQLDriver.URL_PREFIX
  private val URL_REGEX = UJESSQLDriver.URL_REGEX.r

  val HOST = UJESSQLDriver.HOST
  val PORT = UJESSQLDriver.PORT
  val DB_NAME = UJESSQLDriver.DB_NAME
  val PARAMS = UJESSQLDriver.PARAMS

  val USER = UJESSQLDriver.USER
  val PASSWORD = UJESSQLDriver.PASSWORD

  val VERSION = UJESSQLDriver.VERSION
  val DEFAULT_VERSION = UJESSQLDriver.DEFAULT_VERSION
  val MAX_CONNECTION_SIZE = UJESSQLDriver.MAX_CONNECTION_SIZE
  val READ_TIMEOUT = UJESSQLDriver.READ_TIMEOUT
  val ENABLE_DISCOVERY = UJESSQLDriver.ENABLE_DISCOVERY
  val ENABLE_LOADBALANCER = UJESSQLDriver.ENABLE_LOADBALANCER
  val CREATOR = UJESSQLDriver.CREATOR

  val VARIABLE_HEADER = UJESSQLDriver.VARIABLE_HEADER

  def getConnectionParams(connectionParams: String, variableMap: java.util.Map[String, Any]): String = {
    val variables = JavaConversions.mapAsScalaMap(variableMap).map(kv => VARIABLE_HEADER + kv._1 + KV_SPLIT + kv._2).mkString(PARAM_SPLIT)
    if(StringUtils.isNotBlank(connectionParams)) connectionParams + PARAM_SPLIT + variables
    else variables
  }

  def getConnectionParams(version: String, creator: String): String = getConnectionParams(version, creator, 10, 45000)

  def getConnectionParams(version: String, creator: String, maxConnectionSize: Int, readTimeout: Long): String =
    getConnectionParams(version, creator, maxConnectionSize, readTimeout, false, false)

  def getConnectionParams(version: String, creator: String, maxConnectionSize: Int, readTimeout: Long,
                          enableDiscovery: Boolean, enableLoadBalancer: Boolean): String = {
    val sb = new StringBuilder
    if(StringUtils.isNotBlank(version)) sb.append(VERSION).append(KV_SPLIT).append(version)
    if(maxConnectionSize > 0) sb.append(PARAM_SPLIT).append(MAX_CONNECTION_SIZE).append(KV_SPLIT).append(maxConnectionSize)
    if(readTimeout > 0) sb.append(PARAM_SPLIT).append(READ_TIMEOUT).append(KV_SPLIT).append(readTimeout)
    if(enableDiscovery) {
      sb.append(PARAM_SPLIT).append(ENABLE_DISCOVERY).append(KV_SPLIT).append(enableDiscovery)
      if(enableLoadBalancer) sb.append(PARAM_SPLIT).append(ENABLE_LOADBALANCER).append(KV_SPLIT).append(enableLoadBalancer)
    }
    if(sb.startsWith(PARAM_SPLIT)) sb.toString.substring(PARAM_SPLIT.length) else sb.toString
  }

  private[jdbc] val PARAM_SPLIT = UJESSQLDriver.PARAM_SPLIT
  private[jdbc] val KV_SPLIT = UJESSQLDriver.KV_SPLIT

  def main(args: Array[String]): Unit = {

  }
}