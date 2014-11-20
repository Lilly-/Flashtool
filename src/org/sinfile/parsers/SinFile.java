package org.sinfile.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.sinfile.parsers.v3.AddrBlock;
import org.sinfile.parsers.v3.AddrBlocks;
import org.sinfile.parsers.v3.DataHeader;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

public class SinFile {

	File sinfile=null;
	int version=0;
	byte[] hashv3len = {0, 0, 32};
	
	public SinFile(File f) throws IOException {
		sinfile = f;
		
		JBBPParser sinParserV2 = JBBPParser.prepare(
			    "byte multipleHeaders;"
              + "int headerLen;"
              + "byte payloadType;"
              + "short unknown;"
              + "byte memId;"
              + "byte compression;"
              + "int hashLen;"
              + "byte[hashLen] hashBlocks;"
              + "int certLen;"
              + "byte[certLen] cert;"
        );
		
		JBBPParser hashBlocksV2 = JBBPParser.prepare(
	            "block[_] {int offset;"
              + "int length;"
              + "byte hashLen;"
              + "byte[hashLen] crc;}"
		);
		
		JBBPParser sinParserV3 = JBBPParser.prepare(
			    "byte[3] magic;"
              + "int headerLen;"
              + "int payloadType;"
              + "int hashType;"
              + "int reserved;"
              + "int hashLen;"
              + "byte[hashLen] hashBlocks;"
              + "int certLen;"
              + "byte[certLen] cert;"
        );

		JBBPParser dataHeader = JBBPParser.prepare(
				"byte[4] mmcfMagic;"
			  + "int mmcfLen;"
              + "byte[4] gptpMagic;"
			  + "int gptpLen;"
              + "byte[16] uuid;"
              + "byte[mmcfLen-gptpLen] addrList;"
        );

		JBBPParser addrBlocks = JBBPParser.prepare(
				"addrBlocks[_] {byte[4] addrMagic;"
			  + "int addrLen;"
			  + ">long dataOffset;"
              + ">long dataLen;"
			  + ">long fileOffset;"
              + "int hashType;"
              + "byte[addrLen-36] crc;}"
        );

		JBBPBitInputStream sinStream = new JBBPBitInputStream(new FileInputStream(sinfile));
		version = sinStream.readByte();
		if (version==2) {
			org.sinfile.parsers.v2.SinParser sin = sinParserV2.parse(sinStream).mapTo(org.sinfile.parsers.v2.SinParser.class);
			org.sinfile.parsers.v2.HashBlocks blocks = hashBlocksV2.parse(sin.hashBlocks).mapTo(org.sinfile.parsers.v2.HashBlocks.class);
			System.out.println("Version : "+version+"\nMultiple Headers : "+sin.multipleHeaders+"\nHeader Length : "+sin.headerLen+"\nPayLoad Type : "+sin.payloadType+"\nMem Id : "+sin.memId+"\nCompressoin : "+sin.compression+"\nHash Length : "+sin.hashLen);
			System.out.println(sin.certLen);
		}
		if (version==3) {
			org.sinfile.parsers.v3.SinParser sin = sinParserV3.parse(sinStream).mapTo(org.sinfile.parsers.v3.SinParser.class);
			JBBPParser hashBlocksV3 = JBBPParser.prepare(
		            "blocks[_] {int length;"
	              + "byte["+hashv3len[sin.hashType]+"] crc;}"
			);
			org.sinfile.parsers.v3.HashBlocks blocks = hashBlocksV3.parse(sin.hashBlocks).mapTo(org.sinfile.parsers.v3.HashBlocks.class);
			// First hash block seems to be Data header (addr map)
			byte[] dheader = sinStream.readByteArray(blocks.blocks[0].length);
			DataHeader dh = dataHeader.parse(dheader).mapTo(DataHeader.class);
			AddrBlocks addrblocks = addrBlocks.parse(dh.addrList).mapTo(AddrBlocks.class);
			System.out.println("Version : "+version+"\nMagic : "+new String(sin.magic)+"\nHeader Length : "+sin.headerLen+"\nPayLoad Type : "+sin.payloadType+"\nHash type : "+sin.hashType+"\nReserved : "+sin.reserved+"\nHashList Length : "+sin.hashLen+" ("+blocks.blocks.length+" hashblocks) \nCert len : "+sin.certLen);
			System.out.println(addrblocks.addrBlocks[0].dataLen);
		}
	}

	public String getName() {
		return sinfile.getName();
	}

	public int getVersion() {
		return version;
	}

}