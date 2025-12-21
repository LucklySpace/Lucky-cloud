package com.xy.lucky.file.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Objects;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "文件断点下载")
public class OssFileDownloadRange {

    @Schema(description = "range起始位置")
    private Long start;

    @Schema(description = "range结束位置")
    private Long end;

    @Schema(description = "range段长度")
    private Long length;

    @Schema(description = "range段大小")
    private Long total;

    /**
     * Range段构造方法.
     *
     * @param start range起始位置.
     * @param end   range结束位置.
     * @param total range段的长度.
     */
    public OssFileDownloadRange(long start, long end, long total) {
        this.start = start;
        this.end = end;
        this.length = end - start + 1;
        this.total = total;
    }

    /**
     * 获取Range的起始位置.
     *
     * @param value      Range字符串.
     * @param beginIndex Range字符串的起始位置.
     * @param endIndex   Range字符串的结束位置.
     * @return Range的起始位置.
     */
    public static long subLong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * 断点续传下载.
     *
     * @param randomAccessFile 随机访问文件.
     * @param output           输出流.
     * @param fileSize         文件大小.
     * @param start            起始位置.
     * @param length           片段长度.
     * @throws IOException 读写异常.
     */
    private static void copy(RandomAccessFile randomAccessFile, OutputStream output, long fileSize, long start, long length) throws IOException {
        byte[] buffer = new byte[4096];
        int read = 0;
        long transmitted = 0;
        if (fileSize == length) {
            randomAccessFile.seek(start);
            //需要下载的文件长度与文件长度相同，下载整个文件.
            while ((transmitted + read) <= length && (read = randomAccessFile.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                transmitted += read;
            }
            //处理最后不足buff大小的部分
            if (transmitted < length) {
                log.info("最后不足buff大小的部分大小为：" + (length - transmitted));
                read = randomAccessFile.read(buffer, 0, (int) (length - transmitted));
                output.write(buffer, 0, read);
            }
        } else {
            randomAccessFile.seek(start);
            long toRead = length;

            //如果需要读取的片段，比单次读取的4096小，则使用读取片段大小读取
            if (toRead < buffer.length) {
                output.write(buffer, 0, randomAccessFile.read(new byte[(int) toRead]));
                return;
            }
            while ((read = randomAccessFile.read(buffer)) > 0) {
                toRead -= read;
                if (toRead > 0) {
                    output.write(buffer, 0, read);
                } else {
                    output.write(buffer, 0, (int) toRead + read);
                    break;
                }
            }

        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OssFileDownloadRange range = (OssFileDownloadRange) o;
        return start == range.start &&
                end == range.end &&
                length == range.length &&
                total == range.total;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return Objects.hash(prime, start, end, length, total);
    }
}
