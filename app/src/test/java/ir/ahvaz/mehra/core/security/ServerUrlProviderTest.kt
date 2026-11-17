package ir.ahvaz.mehra.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Windows desktop app serves the API over plain HTTP on the hotel LAN
 * (http://<PC-IP>:5050), so [ServerUrlProvider.isLanHost] decides which hosts
 * may use cleartext even in release builds.
 */
class ServerUrlProviderTest {

    @Test fun `private ipv4 ranges are LAN`() {
        assertTrue(ServerUrlProvider.isLanHost("192.168.1.20"))
        assertTrue(ServerUrlProvider.isLanHost("10.0.0.5"))
        assertTrue(ServerUrlProvider.isLanHost("172.16.3.9"))
        assertTrue(ServerUrlProvider.isLanHost("172.31.255.1"))
    }

    @Test fun `loopback link-local and cgnat are LAN`() {
        assertTrue(ServerUrlProvider.isLanHost("127.0.0.1"))
        assertTrue(ServerUrlProvider.isLanHost("169.254.10.10"))
        assertTrue(ServerUrlProvider.isLanHost("100.64.0.2"))
        assertTrue(ServerUrlProvider.isLanHost("::1"))
        assertTrue(ServerUrlProvider.isLanHost("fe80::1"))
        assertTrue(ServerUrlProvider.isLanHost("fd00::abcd"))
    }

    @Test fun `lan hostnames are LAN`() {
        assertTrue(ServerUrlProvider.isLanHost("localhost"))
        assertTrue(ServerUrlProvider.isLanHost("pc-mehra.local"))
        assertTrue(ServerUrlProvider.isLanHost("hotel.lan"))
        assertTrue(ServerUrlProvider.isLanHost("nas.internal"))
    }

    @Test fun `public addresses are not LAN`() {
        assertFalse(ServerUrlProvider.isLanHost("8.8.8.8"))
        assertFalse(ServerUrlProvider.isLanHost("172.32.0.1"))
        assertFalse(ServerUrlProvider.isLanHost("192.169.1.1"))
        assertFalse(ServerUrlProvider.isLanHost("mehra.example.ir"))
        assertFalse(ServerUrlProvider.isLanHost("example.com"))
        assertFalse(ServerUrlProvider.isLanHost(""))
    }
}
