package com.bg.haitao.idCardPicSrv;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.bg.haitao.common.Result;
import com.bg.haitao.common.ResultConvertor;

@Controller
@RequestMapping("/idCardPicSrv")
public class IdCardPicSrvCtrl {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private DefaultLobHandler lobHandler = new DefaultLobHandler();

	@RequestMapping(path = "/showPicsAllAtOnce/{addrId}", method = RequestMethod.GET)
	public void showPicsAllAtOnce(@PathVariable("addrId") String addrId,
			HttpServletResponse response) {
		try {
			IdCardPicInfo idCardPicInfo = jdbcTemplate
					.queryForObject(
							"select identity_frontpic, identity_backpic from user_address where address_id = ?",
							new Object[] { addrId },
							new RowMapper<IdCardPicInfo>() {

								@Override
								public IdCardPicInfo mapRow(ResultSet rs,
										int rowNum) throws SQLException {
									return new IdCardPicInfo(rs
											.getString("identity_frontpic"), rs
											.getString("identity_backpic"));
								}
							});
			if (idCardPicInfo != null) {
				response.setContentType("text/html; charset=utf-8");
				response.getOutputStream().print(
						"<html><head><title>amituofo</title></head><body><img src=\"../showPic/"
								+ idCardPicInfo.getUpperSide()
								+ "?side=0\"/><img src=\"../showPic/"
								+ idCardPicInfo.getReverseSide()
								+ "?side=1\"/></body></html>");
			} else {
				response.setContentType("text/html; charset=utf-8");
				response.getOutputStream()
						.print("<html><head><title>amituofo</title></head><body><h1>No 身份证  associated with this address ["
								+ addrId + "]</h1></body></html>");
			}
		} catch (IOException e) {

		}
	}

	private class IdCardPicInfo {
		private String upperSide;
		private String reverseSide;

		public IdCardPicInfo(String upperSide, String reverseSide) {
			this.upperSide = upperSide;
			this.reverseSide = reverseSide;
		}

		public String getUpperSide() {
			return upperSide;
		}

		public String getReverseSide() {
			return reverseSide;
		}
	}
	
	@RequestMapping(path = "/showPic/{idCard}", method = RequestMethod.GET)
	public void showPic(@PathVariable("idCard") String idCard,
			@QueryParam("side") String side, HttpServletResponse response) {
		int sideInt = 0;
		if (side != null && !side.equals("")) {
			try {
				sideInt = Integer.parseInt(side);
			} catch (NumberFormatException nfe) {

			}
			if (sideInt != 0 && sideInt != 1) {
				sideInt = 0;
			}
		}
		addWaterMark(idCard, String.valueOf(sideInt), response);
	}

	private void addWaterMark(String idCard, String side,
			HttpServletResponse response) {
		ServletOutputStream out = null;
		try {
			out = response.getOutputStream();
			List<InputStream> results = jdbcTemplate.query(
					"select pic_bin from idcard_pics where idcard = " + idCard
							+ " and side = " + side,
					new RowMapper<InputStream>() {

						@Override
						public InputStream mapRow(ResultSet rs, int rowNum)
								throws SQLException {
							return lobHandler.getBlobAsBinaryStream(rs, 1);
						}
					});
			if (results != null) {
				String waterMark = " for haitao only ";

				BufferedImage buffered = ImageIO.read(results.get(0));
				Graphics2D g2d = (Graphics2D) buffered.getGraphics();

				AlphaComposite alphaChannel = AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.5f);
				g2d.setComposite(alphaChannel);
				g2d.setColor(Color.YELLOW);
				g2d.setFont(new Font("Arial", Font.BOLD, 18));
				FontMetrics fontMetrics = g2d.getFontMetrics();
				Rectangle2D rect = fontMetrics.getStringBounds(waterMark, g2d);

				int x = 0, y = 0;
				while (x < buffered.getWidth() || y < buffered.getHeight()) {
					g2d.drawString(waterMark, x, y);
					if (x < buffered.getWidth()) {
						x += rect.getWidth();
					} else {
						x = 0;
						y += rect.getHeight();
					}
				}

				if (out != null) {
					response.setContentType("image/jpeg");
					ImageIO.write(buffered, "jpg", out);
				}
				g2d.dispose();
			}
		} catch (Exception e) {
			if (out != null) {
				response.setContentType("application/json;charset=UTF-8");
				try {
					out.print(ResultConvertor
							.toJson(new Result<Object>(3,
									"id card pic is corrupted or doesn't even exist ever")));
				} catch (IOException e1) {

				}
			}

		}
	}

	@RequestMapping(path = "/upload/{side}", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public String upload(@PathVariable("side") String side,
			@QueryParam("idCard") String idCard,
			@RequestParam("idCardPic") final MultipartFile input) {
		int sideInt = 0;
		try {
			sideInt = Integer.parseInt(side);
		} catch (NumberFormatException nfe) {
			return ResultConvertor.toJson(new Result<Object>(1, "side[" + side
					+ "] should be int"));
		}
		if (sideInt != 0 && sideInt != 1) {
			return ResultConvertor.toJson(new Result<Object>(1, "side[" + side
					+ "] should be 0 or 1"));
		}
		if (idCard == null || idCard.equals("")) {
			StringBuffer sb = new StringBuffer();
			sb.append(System.currentTimeMillis()).append(
					(int) (Math.random() * 100));
			idCard = sb.toString();
		}
		int bytes = 0;
		if (input != null) {
			try {
				if (input.getInputStream() != null) {
					final Integer sideIntFinal = sideInt;
					final String idCardFinal = idCard;
					bytes = (int) input.getSize();
					final int bytesFinal = bytes;
					final InputStream stream = input.getInputStream();
					jdbcTemplate
							.execute(
									"insert into idcard_pics (idcard, side, pic_bin) values (?, ?, ?)",
									new AbstractLobCreatingPreparedStatementCallback(
											lobHandler) {

										@Override
										protected void setValues(
												PreparedStatement ps,
												LobCreator lobCreator)
												throws SQLException {
											ps.setString(1, idCardFinal);
											ps.setInt(2, sideIntFinal);
											lobCreator.setBlobAsBinaryStream(
													ps, 3, stream, bytesFinal);
										}
									});
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {

						}
					}
				}
			} catch (IOException ioe) {
				return ResultConvertor.toJson(new Result<String>(3,
						"upload failed"));
			}
		}
		List<String> extramsg = new ArrayList<String>();
		extramsg.add(idCard);
		return ResultConvertor.toJson(new Result<String>(0, "upload success",
				extramsg));
	}

	@RequestMapping(path = "/delete/{idCard}/{side}", produces = "application/json")
	@ResponseBody
	public String delete(@PathVariable("idCard") String idCard,
			@PathVariable("side") String side) {
		int sideInt = 0;
		try {
			sideInt = Integer.parseInt(side);
		} catch (NumberFormatException nfe) {
			return ResultConvertor.toJson(new Result<Object>(1, "side[" + side
					+ "] should be int"));
		}
		if (sideInt != 0 && sideInt != 1) {
			return ResultConvertor.toJson(new Result<Object>(1, "side[" + side
					+ "] should be 0 or 1"));
		}
		try {
			jdbcTemplate.update("delete from idcard_pics where idcard = "
					+ idCard + " and side = " + side);
		} catch (DataAccessException dae) {
			return ResultConvertor
					.toJson(new Result<Object>(3, "delete failed"));
		}
		return ResultConvertor.toJson(new Result<Object>(0, "delete success"));
	}
}
