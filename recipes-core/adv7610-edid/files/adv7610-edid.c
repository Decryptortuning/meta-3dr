#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/v4l2-dv-timings.h>
#include <linux/v4l2-subdev.h>
#include <linux/videodev2.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#define EDID_BLOCK_SIZE 128

static const uint8_t edid_1080p[EDID_BLOCK_SIZE] = {
	0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x04, 0x21, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x01, 0x18, 0x01, 0x03, 0x80, 0x10, 0x09, 0x78, 0x0a, 0xcf, 0x74, 0xa3, 0x57, 0x4c, 0xb0, 0x23,
	0x09, 0x48, 0x4c, 0x21, 0x08, 0x00, 0x61, 0x40, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
	0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02, 0x3a, 0x80, 0x18, 0x71, 0x38, 0x2d, 0x40, 0x58, 0x2c,
	0x45, 0x00, 0xe0, 0x0e, 0x11, 0x00, 0x00, 0x1e, 0x01, 0x1d, 0x00, 0x72, 0x51, 0xd0, 0x1e, 0x20,
	0x6e, 0x28, 0x55, 0x00, 0xe0, 0x0e, 0x11, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x00, 0x0f, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4d,
};

static const uint8_t edid_720p[EDID_BLOCK_SIZE] = {
	0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x04, 0x21, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x01, 0x18, 0x01, 0x03, 0x80, 0x10, 0x09, 0x78, 0x0a, 0xcf, 0x74, 0xa3, 0x57, 0x4c, 0xb0, 0x23,
	0x09, 0x48, 0x4c, 0x21, 0x08, 0x00, 0x61, 0x40, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
	0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x1d, 0x00, 0x72, 0x51, 0xd0, 0x1e, 0x20, 0x6e, 0x28,
	0x55, 0x00, 0xe0, 0x0e, 0x11, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x00, 0x0f, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x23,
};

static void usage(const char *argv0)
{
	fprintf(stderr,
	        "Usage: %s [--device /dev/v4l-subdevX] [--edid 1080p|720p|disable] [--edid-file FILE] [--wait-ms N]\n",
	        argv0);
}

static bool str_has_ci(const char *haystack, const char *needle)
{
	size_t needle_len = strlen(needle);
	size_t hay_len = strlen(haystack);

	if (!needle_len || needle_len > hay_len)
		return false;

	for (size_t i = 0; i + needle_len <= hay_len; i++) {
		if (strncasecmp(haystack + i, needle, needle_len) == 0)
			return true;
	}
	return false;
}

static char *read_first_line(const char *path)
{
	FILE *fp = fopen(path, "r");
	if (!fp)
		return NULL;

	char *line = NULL;
	size_t cap = 0;
	ssize_t nread = getline(&line, &cap, fp);
	fclose(fp);

	if (nread <= 0) {
		free(line);
		return NULL;
	}

	while (nread > 0 && (line[nread - 1] == '\n' || line[nread - 1] == '\r'))
		line[--nread] = '\0';

	return line;
}

static char *autodetect_adv76xx_subdev(void)
{
	DIR *dir = opendir("/sys/class/video4linux");
	if (!dir)
		return NULL;

	struct dirent *de;
	while ((de = readdir(dir)) != NULL) {
		if (strncmp(de->d_name, "v4l-subdev", 10) != 0)
			continue;

		char name_path[256];
		snprintf(name_path, sizeof(name_path), "/sys/class/video4linux/%s/name", de->d_name);
		char *name = read_first_line(name_path);
		if (!name)
			continue;

		bool match = str_has_ci(name, "adv7610") || str_has_ci(name, "adv7604") || str_has_ci(name, "adv76");
		free(name);
		if (!match)
			continue;

		char *devnode = malloc(64);
		if (!devnode)
			break;
		snprintf(devnode, 64, "/dev/%s", de->d_name);
		closedir(dir);
		return devnode;
	}

	closedir(dir);
	return NULL;
}

static int read_entire_file(const char *path, uint8_t **out, size_t *out_len)
{
	int fd = open(path, O_RDONLY);
	if (fd < 0)
		return -1;

	struct stat st;
	if (fstat(fd, &st) != 0) {
		close(fd);
		return -1;
	}

	if (st.st_size <= 0) {
		close(fd);
		errno = EINVAL;
		return -1;
	}

	uint8_t *buf = malloc((size_t)st.st_size);
	if (!buf) {
		close(fd);
		errno = ENOMEM;
		return -1;
	}

	ssize_t off = 0;
	while (off < st.st_size) {
		ssize_t r = read(fd, buf + off, (size_t)st.st_size - (size_t)off);
		if (r < 0) {
			free(buf);
			close(fd);
			return -1;
		}
		if (r == 0)
			break;
		off += r;
	}

	close(fd);

	*out = buf;
	*out_len = (size_t)off;
	return 0;
}

static int set_edid_on_pad(int fd, uint32_t pad, const uint8_t *edid_data, uint32_t blocks)
{
	struct v4l2_edid edid = {0};
	edid.pad = pad;
	edid.start_block = 0;
	edid.blocks = blocks;
	edid.edid = (uint8_t *)edid_data;

	return ioctl(fd, VIDIOC_SUBDEV_S_EDID, &edid);
}

static int set_edid(int fd, const uint8_t *edid_data, size_t edid_len)
{
	uint32_t blocks = 0;
	if (edid_len == 0) {
		blocks = 0;
	} else {
		if (edid_len % EDID_BLOCK_SIZE != 0) {
			errno = EINVAL;
			return -1;
		}
		blocks = (uint32_t)(edid_len / EDID_BLOCK_SIZE);
		if (blocks == 0 || blocks > 256) {
			errno = EINVAL;
			return -1;
		}
	}

	if (set_edid_on_pad(fd, 0, edid_data, blocks) == 0)
		return 0;

	if (errno == EINVAL) {
		if (set_edid_on_pad(fd, 1, edid_data, blocks) == 0)
			return 0;
	}

	return -1;
}

static int wait_for_dv_timings(int fd, int timeout_ms)
{
	if (timeout_ms <= 0)
		return 0;

	const int step_ms = 100;
	int waited_ms = 0;

	for (;;) {
		struct v4l2_dv_timings t = {0};
		if (ioctl(fd, VIDIOC_SUBDEV_QUERY_DV_TIMINGS, &t) == 0)
			return 0;

		if (errno == EINVAL)
			return -1;

		if (waited_ms >= timeout_ms) {
			errno = ETIMEDOUT;
			return -1;
		}

		usleep(step_ms * 1000);
		waited_ms += step_ms;
	}
}

int main(int argc, char **argv)
{
	const char *device = NULL;
	const char *edid_mode = "1080p";
	const char *edid_file = NULL;
	int wait_ms = 0;

	for (int i = 1; i < argc; i++) {
		if (strcmp(argv[i], "--device") == 0 && i + 1 < argc) {
			device = argv[++i];
		} else if (strcmp(argv[i], "--edid") == 0 && i + 1 < argc) {
			edid_mode = argv[++i];
		} else if (strcmp(argv[i], "--edid-file") == 0 && i + 1 < argc) {
			edid_file = argv[++i];
		} else if (strcmp(argv[i], "--wait-ms") == 0 && i + 1 < argc) {
			wait_ms = atoi(argv[++i]);
		} else if (strcmp(argv[i], "-h") == 0 || strcmp(argv[i], "--help") == 0) {
			usage(argv[0]);
			return 0;
		} else {
			usage(argv[0]);
			return 2;
		}
	}

	char *autodetected = NULL;
	if (!device) {
		autodetected = autodetect_adv76xx_subdev();
		device = autodetected;
	}

	if (!device) {
		fprintf(stderr, "adv7610-edid: no ADV76xx subdevice found; skipping\n");
		return 0;
	}

	uint8_t *file_edid = NULL;
	size_t file_edid_len = 0;
	const uint8_t *edid_data = NULL;
	size_t edid_len = 0;

	if (edid_file) {
		if (read_entire_file(edid_file, &file_edid, &file_edid_len) != 0) {
			fprintf(stderr, "adv7610-edid: failed to read EDID file '%s': %s\n", edid_file, strerror(errno));
			free(autodetected);
			return 1;
		}
		edid_data = file_edid;
		edid_len = file_edid_len;
	} else if (strcmp(edid_mode, "1080p") == 0) {
		edid_data = edid_1080p;
		edid_len = sizeof(edid_1080p);
	} else if (strcmp(edid_mode, "720p") == 0) {
		edid_data = edid_720p;
		edid_len = sizeof(edid_720p);
	} else if (strcmp(edid_mode, "disable") == 0) {
		edid_data = NULL;
		edid_len = 0;
	} else {
		fprintf(stderr, "adv7610-edid: unknown --edid mode '%s'\n", edid_mode);
		free(autodetected);
		return 2;
	}

	int fd = open(device, O_RDWR);
	if (fd < 0) {
		fprintf(stderr, "adv7610-edid: open('%s') failed: %s\n", device, strerror(errno));
		free(file_edid);
		free(autodetected);
		return 1;
	}

	if (set_edid(fd, edid_data, edid_len) != 0) {
		fprintf(stderr, "adv7610-edid: failed to set EDID on %s: %s\n", device, strerror(errno));
		close(fd);
		free(file_edid);
		free(autodetected);
		return 1;
	}

	if (wait_ms > 0) {
		if (wait_for_dv_timings(fd, wait_ms) != 0) {
			fprintf(stderr, "adv7610-edid: no HDMI lock after %dms (%s)\n", wait_ms, strerror(errno));
		}
	}

	close(fd);
	free(file_edid);
	free(autodetected);
	return 0;
}
