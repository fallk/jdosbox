package jdos.win.builtin.ddraw;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.gui.Main;
import jdos.hardware.Memory;
import jdos.win.Console;
import jdos.win.Win;
import jdos.win.builtin.HandlerBase;
import jdos.win.kernel.VideoMemory;
import jdos.win.kernel.WinCallback;
import jdos.win.utils.Error;
import jdos.win.utils.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class IDirectDrawSurface extends IUnknown {
    static final int DDSCAPS_ALPHA =                0x00000002; /* surface contains alpha information */
    static final int DDSCAPS_BACKBUFFER =           0x00000004; /* this surface is a backbuffer */
    static final int DDSCAPS_COMPLEX =              0x00000008; /* complex surface structure */
    static final int DDSCAPS_FLIP =                 0x00000010; /* part of surface flipping structure */
    static final int DDSCAPS_FRONTBUFFER =          0x00000020; /* this surface is the frontbuffer surface */
    static final int DDSCAPS_OFFSCREENPLAIN =       0x00000040; /* this is a plain offscreen surface */
    static final int DDSCAPS_OVERLAY =              0x00000080; /* overlay */
    static final int DDSCAPS_PALETTE =              0x00000100; /* palette objects can be created and attached to us */
    static final int DDSCAPS_PRIMARYSURFACE =       0x00000200; /* primary surface (the one the user looks at currently)(right eye)*/
    static final int DDSCAPS_PRIMARYSURFACELEFT	=   0x00000400; /* primary surface for left eye */
    static final int DDSCAPS_SYSTEMMEMORY =         0x00000800; /* surface exists in systemmemory */
    static final int DDSCAPS_TEXTURE =              0x00001000; /* surface can be used as a texture */
    static final int DDSCAPS_3DDEVICE =             0x00002000; /* surface may be destination for 3d rendering */
    static final int DDSCAPS_VIDEOMEMORY =          0x00004000; /* surface exists in videomemory */
    static final int DDSCAPS_VISIBLE =              0x00008000; /* surface changes immediately visible */
    static final int DDSCAPS_WRITEONLY =            0x00010000; /* write only surface */
    static final int DDSCAPS_ZBUFFER =              0x00020000; /* zbuffer surface */
    static final int DDSCAPS_OWNDC =                0x00040000; /* has its own DC */
    static final int DDSCAPS_LIVEVIDEO =            0x00080000; /* surface should be able to receive live video */
    static final int DDSCAPS_HWCODEC =              0x00100000; /* should be able to have a hw codec decompress stuff into it */
    static final int DDSCAPS_MODEX =                0x00200000; /* mode X (320x200 or 320x240) surface */
    static final int DDSCAPS_MIPMAP =               0x00400000; /* one mipmap surface (1 level) */
    static final int DDSCAPS_RESERVED2 =            0x00800000;
    static final int DDSCAPS_ALLOCONLOAD =          0x04000000; /* memory allocation delayed until Load() */
    static final int DDSCAPS_VIDEOPORT =            0x08000000; /* Indicates that the surface will receive data from a video port */
    static final int DDSCAPS_LOCALVIDMEM =          0x10000000; /* surface is in local videomemory */
    static final int DDSCAPS_NONLOCALVIDMEM =       0x20000000; /* surface is in nonlocal videomemory */
    static final int DDSCAPS_STANDARDVGAMODE =      0x40000000; /* surface is a standard VGA mode surface (NOT ModeX) */
    static final int DDSCAPS_OPTIMIZED =            0x80000000;

    static final int DDLOCK_SURFACEMEMORYPTR =  0x00000000;
    static final int DDLOCK_WAIT =              0x00000001;
    static final int DDLOCK_EVENT =             0x00000002;
    static final int DDLOCK_READONLY =          0x00000010;
    static final int DDLOCK_WRITEONLY =         0x00000020;
    static final int DDLOCK_NOSYSLOCK =         0x00000800;
    static final int DDLOCK_NOOVERWRITE =       0x00001000;
    static final int DDLOCK_DISCARDCONTENTS =   0x00002000;

    static int FLAGS_CAPS2 = 0x00000001;
    static int FLAGS_DESC2 = 0x00000002;
    static int FLAGS_LOCKED = 0x00000004;

    static int OFFSET_FLAGS = 0;
    static int OFFSET_PALETTE = 4;
    static int OFFSET_MEMORY = 8;
    static int OFFSET_BACK_BUFFER = 12;
    static int OFFSET_DC = 16;
    static int OFFSET_IMAGE = 20;

    // doesn't include description since that gets computed on the fly
    static int DATA_SIZE = 24;

    static int OFFSET_DESC = DATA_SIZE;


    public static boolean isCap2(int This) {
        int flags = getData(This, OFFSET_FLAGS);
        return (flags & FLAGS_CAPS2) != 0;
    }

    public static boolean isDesc2(int This) {
        int flags = getData(This, OFFSET_FLAGS);
        return (flags & FLAGS_DESC2) != 0;
    }

    public static BufferedImage getImage(int This, boolean create) {
        int index = getData(This, OFFSET_IMAGE);
        BufferedImage image = null;
        if (index == 0) {
            if (create) {
                int address = getData(This, OFFSET_MEMORY);
                int lpDDPalette = getData(This, OFFSET_PALETTE);
                int[] palette = null;
                if (lpDDPalette != 0) {
                    palette = new int[256];
                    for (int i=0;i<palette.length;i++) {
                        palette[i] = getData(lpDDPalette, IDirectDrawPalette.OFFSET_COLOR_DATA+4*i);
                    }
                }
                image = Pixel.createImage(address, getData(This, OFFSET_DESC+0x54),  palette, getData(This, OFFSET_DESC+0x0C), getData(This, OFFSET_DESC+0x08));
                if ((getData(This, OFFSET_FLAGS) & FLAGS_LOCKED)==0) {
                    WinObject object = WinSystem.createWinObject();
                    object.data = image;
                    setData(This, OFFSET_IMAGE, object.getHandle());
                }
            }
        } else {
            WinObject object = WinSystem.getObject(index);
            image = (BufferedImage)object.data;
        }
        return image;
    }

    public static void clearImage(int This) {
        int index = getData(This, OFFSET_IMAGE);
        if (index>0) {
            setData(This, OFFSET_IMAGE, 0);
            WinObject object = WinSystem.getObject(index);
            object.close();
        }
    }

    public static void saveImage(int This, BufferedImage image) {
        int address = getData(This, OFFSET_MEMORY);
        Pixel.writeImage(address, image, getData(This, OFFSET_DESC+0x54), getData(This, OFFSET_DESC+0x0C), getData(This, OFFSET_DESC+0x08));
    }

    // while locked the surface will not cache a BufferedImage object, this will really slow things down, but allows
    // apps to access surface memory directly, via lock or getDC
    public static void lock(int This) {
        setData(This, OFFSET_FLAGS, getData(This, OFFSET_FLAGS) | FLAGS_LOCKED);
    }

    public static void unlock(int This) {
        setData(This, OFFSET_FLAGS, getData(This, OFFSET_FLAGS) & ~FLAGS_LOCKED);
    }

    public static int create(int pDirectDraw, int pDesc) {
        return create("IDirectDrawSurface", pDirectDraw, pDesc, 0);
    }

    static private Callback.Handler CleanUp = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.CleanUp";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int palette = getData(This, OFFSET_PALETTE);
            if (palette != 0) {
                Release(palette);
            }
            int backBuffer = getData(This, OFFSET_BACK_BUFFER);
            if (backBuffer != 0) {
                Release(backBuffer);
            }
            int hdc = getData(This, OFFSET_DC);
            if (hdc != 0) {
                WinDC dc = (WinDC)WinSystem.getObject(hdc);
                dc.close();
            }
            clearImage(This);
        }
    };

    public static int cleanupCallback;

    public static int create(String name, int pDirectDraw, int pDesc, int flags) {
        int vtable = getVTable(name);
        if (vtable == 0) {
            vtable = createVTable();
            cleanupCallback = WinCallback.addCallback(CleanUp);
        }
        int result = allocate(vtable, DDSurfaceDesc.SIZE2+12, cleanupCallback);
        int descSize =  (flags & FLAGS_DESC2)!=0?DDSurfaceDesc.SIZE2:DDSurfaceDesc.SIZE;
        Memory.mem_memcpy(result+ OFFSET_DATA_START +OFFSET_DESC, pDesc, descSize);

        DDSurfaceDesc d = new DDSurfaceDesc(pDesc, (flags & FLAGS_DESC2)!=0);

        int width=0;
        int height=0;
        int caps = d.ddsCaps;

        if ((d.ddsCaps & DDSCAPS_PRIMARYSURFACE)!=0) {
            width = IDirectDraw.getWidth(pDirectDraw);
            height = IDirectDraw.getHeight(pDirectDraw);
            caps |= DDSCAPS_VIDEOMEMORY;
            caps |= DDSCAPS_VISIBLE;
            caps |= DDSCAPS_LOCALVIDMEM;
        } else if ((d.ddsCaps & DDSCAPS_OFFSCREENPLAIN)!=0) {
            if ((d.dwFlags & DDSurfaceDesc.DDSD_WIDTH) == 0) {
                Win.panic(name+".CreateSurface DDSD_WIDTH must be specified when using DDSCAPS_OFFSCREENPLAIN");
            }
            if ((d.dwFlags & DDSurfaceDesc.DDSD_HEIGHT) == 0) {
                Win.panic(name+".CreateSurface DDSD_HEIGHT must be specified when using DDSCAPS_OFFSCREENPLAIN");
            }
            if ((d.dwFlags & DDSurfaceDesc.DDSD_BACKBUFFERCOUNT)!=0) {
                Win.panic(name+".CreateSurface was expecting DDSD_BACKBUFFERCOUNT with DDSCAPS_OFFSCREENPLAIN");
            }
            width = d.dwWidth;
            height = d.dwHeight;
            caps &= ~DDSCAPS_VISIBLE;
        } else {
            Win.panic(name+".CreateSurface currently DDSCAPS_PRIMARYSURFACE or DDSCAPS_OFFSCREENPLAIN must be specified");
        }

        int bpp = IDirectDraw.getBPP(pDirectDraw);
        int amount = width*height*bpp/8;
        int memory;

        if ((d.ddsCaps & DDSCAPS_VIDEOMEMORY)!=0) {
            if ((d.ddsCaps & DDSCAPS_PRIMARYSURFACE)!=0) {
                memory = VideoMemory.mapVideoRAM(amount);
            } else {
                memory = WinSystem.getCurrentProcess().heap.alloc(amount, true);
            }
            caps |= DDSCAPS_LOCALVIDMEM;
        } else {
            memory = WinSystem.getCurrentProcess().heap.alloc(amount, true);
        }
        Memory.mem_zero(memory, amount);
        setData(result, OFFSET_MEMORY, memory);
        setData(result, OFFSET_DESC, descSize);
        setData(result, OFFSET_DESC+0x04, DDSurfaceDesc.DDSD_CAPS|DDSurfaceDesc.DDSD_HEIGHT|DDSurfaceDesc.DDSD_WIDTH|DDSurfaceDesc.DDSD_PITCH|DDSurfaceDesc.DDSD_PIXELFORMAT);
        setData(result, OFFSET_DESC+0x08, height);
        setData(result, OFFSET_DESC+0x0C, width);
        setData(result, OFFSET_DESC+0x10, width); // pitch

        setData(result, OFFSET_DESC+0x48, DDPixelFormat.SIZE);
        int pfFlags = DDPixelFormat.DDPF_RGB;
        if (bpp == 8) {
            caps|=DDSCAPS_PALETTE;
            pfFlags|=DDPixelFormat.DDPF_PALETTEINDEXED8;
        } else if (bpp == 16) {
            setData(result, OFFSET_DESC+0x58, 0xF800);
            setData(result, OFFSET_DESC+0x5C, 0x07E0);
            setData(result, OFFSET_DESC+0x60, 0x001F);
        } else if (bpp == 24) {
            setData(result, OFFSET_DESC+0x58, 0xFF0000);
            setData(result, OFFSET_DESC+0x5C, 0x00FF00);
            setData(result, OFFSET_DESC+0x60, 0x0000FF);
        } else if (bpp == 32) {
            setData(result, OFFSET_DESC+0x58, 0x00FF0000);
            setData(result, OFFSET_DESC+0x5C, 0x0000FF00);
            setData(result, OFFSET_DESC+0x60, 0x000000FF);
            setData(result, OFFSET_DESC+0x64, 0xFF000000);
        }
        setData(result, OFFSET_DESC+0x4C, pfFlags);
        setData(result, OFFSET_DESC+0x54, bpp);

        if ((d.dwFlags & DDSurfaceDesc.DDSD_BACKBUFFERCOUNT)!=0) {
            if (d.dwBackBufferCount == 1) {
                if ((d.ddsCaps & DDSCAPS_COMPLEX)==0) {
                    Win.panic(name+".CreateSurface wasn't expecting a back buffer without DDSCAPS_COMPLEX");
                }
                // The back buffer will not contain child surfaces and it is never visible on the screen
                setData(result, OFFSET_DESC+0x68, caps & ~(DDSCAPS_VISIBLE | DDSCAPS_COMPLEX));  // :TODO: just guessing what the backbuffer flags look like, need to investigate
                int backBuffer = create(name, pDirectDraw, result+OFFSET_DATA_START+OFFSET_DESC,flags);
                setData(result, OFFSET_BACK_BUFFER, backBuffer);
                // Set after we create the back buffer
                setData(result, OFFSET_DESC+0x04, getData(result, OFFSET_DESC+0x04)|DDSurfaceDesc.DDSD_BACKBUFFERCOUNT);
                setData(result, OFFSET_DESC+0x14, 1);
            } else if (d.dwBackBufferCount != 1) {
                Win.panic(name+".CreateSurface does not currently support more than one back buffer");
            }
        }
        setData(result, OFFSET_DESC+0x68, caps);
        return result;
    }

    static int VTABLE_COUNT = 33;

    static private int createVTable() {
        int address = allocateVTable("IDirectDrawSurface", VTABLE_COUNT);
        addIDirectDrawSurface(address);
        return address;
    }

    static int addIDirectDrawSurface(int address) {
        address = addIUnknown(address);

        address = add(address, AddAttachedSurface);
        address = add(address, AddOverlayDirtyRect);
        address = add(address, Blt);
        address = add(address, BltBatch);
        address = add(address, BltFast);
        address = add(address, DeleteAttachedSurface);
        address = add(address, EnumAttachedSurfaces);
        address = add(address, EnumOverlayZOrders);
        address = add(address, Flip);
        address = add(address, GetAttachedSurface);
        address = add(address, GetBltStatus);
        address = add(address, GetCaps);
        address = add(address, GetClipper);
        address = add(address, GetColorKey);
        address = add(address, GetDC);
        address = add(address, GetFlipStatus);
        address = add(address, GetOverlayPosition);
        address = add(address, GetPalette);
        address = add(address, GetPixelFormat);
        address = add(address, GetSurfaceDesc);
        address = add(address, Initialize);
        address = add(address, IsLost);
        address = add(address, Lock);
        address = add(address, ReleaseDC);
        address = add(address, Restore);
        address = add(address, SetClipper);
        address = add(address, SetColorKey);
        address = add(address, SetOverlayPosition);
        address = add(address, SetPalette);
        address = add(address, Unlock);
        address = add(address, UpdateOverlay);
        address = add(address, UpdateOverlayDisplay);
        address = add(address, UpdateOverlayZOrder);
        return address;
    }

    // HRESULT AddAttachedSurface(this, LPDIRECTDRAWSURFACE lpDDSAttachedSurface)
    static private Callback.Handler AddAttachedSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.AddAttachedSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSAttachedSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };
    
    // HRESULT AddOverlayDirtyRect(this, LPRECT lpRect)
    static private Callback.Handler AddOverlayDirtyRect = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.AddOverlayDirtyRect";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpRect = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Blt(this, LPRECT lpDestRect, LPDIRECTDRAWSURFACE lpDDSrcSurface, LPRECT lpSrcRect, DWORD dwFlags, LPDDBLTFX lpDDBltFx)
    static private Callback.Handler Blt = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Blt";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDestRect = CPU.CPU_Pop32();
            int lpDDSrcSurface = CPU.CPU_Pop32();
            int lpSrcRect = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDBltFx = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT BltBatch(this, LPDDBLTBATCH lpDDBltBatch, DWORD dwCount, DWORD dwFlags)
    static private Callback.Handler BltBatch = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.BltBatch";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDBltBatch = CPU.CPU_Pop32();
            int dwCount = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT BltFast(this, DWORD dwX, DWORD dwY, LPDIRECTDRAWSURFACE lpDDSrcSurface, LPRECT lpSrcRect, DWORD dwTrans)
    static private Callback.Handler BltFast = new HandlerBase() {
        static private final int DDBLTFAST_NOCOLORKEY =     0x00000000;
        static private final int DDBLTFAST_SRCCOLORKEY =    0x00000001;
        static private final int DDBLTFAST_DESTCOLORKEY =   0x00000002;
        static private final int DDBLTFAST_WAIT =           0x00000010;
        static private final int DDBLTFAST_DONOTWAIT =      0x00000020;

        public java.lang.String getName() {
            return "IDirectDrawSurface.BltFast";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwX = CPU.CPU_Pop32();
            int dwY = CPU.CPU_Pop32();
            int lpDDSrcSurface = CPU.CPU_Pop32();
            int lpSrcRect = CPU.CPU_Pop32();
            int dwTrans = CPU.CPU_Pop32();
            BufferedImage dest = getImage(This, true);
            BufferedImage src = getImage(lpDDSrcSurface, true);

            Graphics g = dest.getGraphics();
            int srcX1 = Memory.mem_readd(lpSrcRect);
            int srcY1 = Memory.mem_readd(lpSrcRect+4);
            int srcX2 = Memory.mem_readd(lpSrcRect+8);
            int srcY2 = Memory.mem_readd(lpSrcRect+12);
            int width = srcX2-srcX1;
            int height = srcY2-srcY1;
            g.drawImage(src, dwX, dwY, dwX+width, dwY+height, srcX1, srcY1, srcX2, srcY2, null);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT DeleteAttachedSurface(this, DWORD dwFlags, LPDIRECTDRAWSURFACE lpDDSAttachedSurface)
    static private Callback.Handler DeleteAttachedSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.DeleteAttachedSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSAttachedSurface = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumAttachedSurfaces(this, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpEnumSurfacesCallback)
    static private Callback.Handler EnumAttachedSurfaces = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.EnumAttachedSurfaces";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpEnumSurfacesCallback = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT EnumOverlayZOrders(this, DWORD dwFlags, LPVOID lpContext, LPDDENUMSURFACESCALLBACK lpfnCallback)
    static private Callback.Handler EnumOverlayZOrders = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.EnumOverlayZOrders";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpContext = CPU.CPU_Pop32();
            int lpfnCallback = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Flip(this, LPDIRECTDRAWSURFACE lpDDSurfaceTargetOverride, DWORD dwFlags)
    static private Callback.Handler Flip = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Flip";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurfaceTargetOverride = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();

            // :TODO: this needs more work
            BufferedImage src = getImage(getData(This, OFFSET_BACK_BUFFER), true);
            Main.drawImage(src);
        }
    };

    // HRESULT GetAttachedSurface(this, LPDDSCAPS lpDDSCaps, LPDIRECTDRAWSURFACE *lplpDDAttachedSurface)
    static private Callback.Handler GetAttachedSurface = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetAttachedSurface";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSCaps = CPU.CPU_Pop32();
            int lplpDDAttachedSurface = CPU.CPU_Pop32();
            if (lpDDSCaps == 0 || lplpDDAttachedSurface == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            } else {
                int caps = Memory.mem_readd(lpDDSCaps);
                if (caps == DDSCAPS_BACKBUFFER) {
                    CPU_Regs.reg_eax.dword = Error.S_OK;
                    Memory.mem_writed(lplpDDAttachedSurface, getData(This, OFFSET_BACK_BUFFER));
                } else {
                    Win.panic(getName()+" currently only supports the flag DDSCAPS_BACKBUFFER");
                }
            }
        }
    };

    // HRESULT GetBltStatus(this, DWORD dwFlags)
    static private Callback.Handler GetBltStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetBltStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetCaps(this, LPDDSCAPS lpDDSCaps)
    static private Callback.Handler GetCaps = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetCaps";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSCaps = CPU.CPU_Pop32();
            if (lpDDSCaps == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            } else {
                Memory.mem_writed(lpDDSCaps, Memory.mem_readd(This+ OFFSET_DATA_START +OFFSET_DESC+0x68));
                if (isCap2(This)) {
                    Memory.mem_writed(lpDDSCaps+4, Memory.mem_readd(This+ OFFSET_DATA_START +OFFSET_DESC+0x68+4));
                    Memory.mem_writed(lpDDSCaps+8, Memory.mem_readd(This+ OFFSET_DATA_START +OFFSET_DESC+0x68+8));
                    Memory.mem_writed(lpDDSCaps+12, Memory.mem_readd(This+ OFFSET_DATA_START +OFFSET_DESC+0x68+12));
                }
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT GetClipper(this, LPDIRECTDRAWCLIPPER *lplpDDClipper)
    static private Callback.Handler GetClipper = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetClipper";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpDDClipper = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetColorKey(this, DWORD dwFlags, LPDDCOLORKEY lpDDColorKey)
    static private Callback.Handler GetColorKey = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetColorKey";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDColorKey = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetDC(this, HDC *lphDC)
    static private Callback.Handler GetDC = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetDC";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lphDC = CPU.CPU_Pop32();
            if (lphDC == 0) {
                CPU_Regs.reg_eax.dword = Error.DDERR_INVALIDPARAMS;
            } else {
                int hdc = getData(This, OFFSET_DC);
                if (hdc == 0) {
                    int[] palette = null;
                    int lpDDPalette = getData(This, OFFSET_PALETTE);
                    if (lpDDPalette != 0) {
                        palette = new int[256];
                        for (int i=0;i<palette.length;i++) {
                            palette[i] = getData(lpDDPalette, IDirectDrawPalette.OFFSET_COLOR_DATA+4*i);
                        }
                    }
                    WinDC dc = WinSystem.createDC(null, getData(This, OFFSET_MEMORY), getData(This, OFFSET_DESC+0x0C), getData(This, OFFSET_DESC+0x08), palette);
                    hdc = dc.getHandle();
                    setData(This, OFFSET_DC, hdc);
                } else {
                    WinDC dc = (WinDC)WinSystem.getObject(hdc);
                    dc.open();
                }
                Memory.mem_writed(lphDC, hdc);
                lock(This);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT GetFlipStatus(this, DWORD dwFlags)
    static private Callback.Handler GetFlipStatus = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetFlipStatus";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetOverlayPosition(this, LPLONG lplX, LPLONG lplY)
    static private Callback.Handler GetOverlayPosition = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetOverlayPosition";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplX = CPU.CPU_Pop32();
            int lplY = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetPalette(this, LPDIRECTDRAWPALETTE *lplpDDPalette)
    static private Callback.Handler GetPalette = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetPalette";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lplpDDPalette = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetPixelFormat(this, LPDDPIXELFORMAT lpDDPixelFormat)
    static private Callback.Handler GetPixelFormat = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetPixelFormat";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDPixelFormat = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT GetSurfaceDesc(this, LPDDSURFACEDESC lpDDSurfaceDesc)
    static private Callback.Handler GetSurfaceDesc = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.GetSurfaceDesc";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            int size = Memory.mem_readd(This+OFFSET_DATA_START +OFFSET_DESC);
            if (lpDDSurfaceDesc==0) {
                CPU_Regs.reg_eax.dword = Error.DDERR_INVALIDPARAMS;
            } else if (Memory.mem_readd(lpDDSurfaceDesc) != size) {
                System.out.println(getName()+": wrong size."+Memory.mem_readd(lpDDSurfaceDesc)+" does not equal "+size);
                CPU_Regs.reg_eax.dword = Error.DDERR_INVALIDPARAMS;
            } else {
                Memory.mem_memcpy(lpDDSurfaceDesc, This+OFFSET_DATA_START +OFFSET_DESC, size);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT Initialize(this, LPDIRECTDRAW lpDD, LPDDSURFACEDESC lpDDSurfaceDesc)
    static private Callback.Handler Initialize = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Initialize";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDD = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT IsLost(this)
    static private Callback.Handler IsLost = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.IsLost";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT Lock(this, LPRECT lpDestRect, LPDDSURFACEDESC lpDDSurfaceDesc, DWORD dwFlags, HANDLE hEvent)
    static private Callback.Handler Lock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Lock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDestRect = CPU.CPU_Pop32();
            int lpDDSurfaceDesc = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int hEvent = CPU.CPU_Pop32();
            if ((dwFlags & DDLOCK_EVENT)!=0) {
                Console.out(getName()+" flag DDLOCK_EVENT not implemented yet ");
                notImplemented();
            }
            if ((dwFlags & DDLOCK_NOSYSLOCK)!=0) {
                Console.out(getName()+" flag DDLOCK_NOSYSLOCK not implemented yet ");
                notImplemented();
            }
            if ((dwFlags & DDLOCK_NOOVERWRITE)!=0) {
                Console.out(getName()+" flag DDLOCK_NOOVERWRITE not implemented yet ");
                notImplemented();
            }
            if ((dwFlags & DDLOCK_DISCARDCONTENTS)!=0) {
                Console.out(getName()+" flag DDLOCK_DISCARDCONTENTS not implemented yet ");
                notImplemented();
            }
            if (lpDDSurfaceDesc == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
                return;
            }
            int address = getData(This, OFFSET_MEMORY);
            Memory.mem_memcpy(lpDDSurfaceDesc, This+ OFFSET_DATA_START +OFFSET_DESC, isDesc2(This)?DDSurfaceDesc.SIZE2:DDSurfaceDesc.SIZE);
            Memory.mem_writed(lpDDSurfaceDesc+0x24, address);

            BufferedImage image = getImage(This, false);
            if (image != null)
                saveImage(This, image);
            clearImage(This);
            lock(This);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT ReleaseDC(this, HDC hDC)
    static private Callback.Handler ReleaseDC = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.ReleaseDC";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int hDC = CPU.CPU_Pop32();
            if (hDC != getData(This, OFFSET_DC)) {
                CPU_Regs.reg_eax.dword = Error.DDERR_INVALIDPARAMS;
            } else {
                WinDC dc = (WinDC)WinSystem.getObject(hDC);
                dc.close();
                unlock(This);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT Restore(this)
    static private Callback.Handler Restore = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Restore";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            // no need to do anything here since we never unload it from video memory
        }
    };

    // HRESULT SetClipper(this, LPDIRECTDRAWCLIPPER lpDDClipper)
    static private Callback.Handler SetClipper = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetClipper";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int LPDIRECTDRAWCLIPPER = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetColorKey(this, DWORD dwFlags, LPDDCOLORKEY lpDDColorKey)
    static private Callback.Handler SetColorKey = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetColorKey";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDColorKey = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetOverlayPosition(this, LONG lX, LONG lY)
    static private Callback.Handler SetOverlayPosition = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetOverlayPosition";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lX = CPU.CPU_Pop32();
            int lY = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT SetPalette(this, LPDIRECTDRAWPALETTE lpDDPalette)
    static private Callback.Handler SetPalette = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.SetPalette";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpDDPalette = CPU.CPU_Pop32();
            if (lpDDPalette == 0) {
                CPU_Regs.reg_eax.dword = Error.E_POINTER;
            } else {
                // :TODO: not sure if I should copy the palette or reference it
                AddRef(lpDDPalette); // :TODO: this will be leaked
                setData(This, OFFSET_PALETTE, lpDDPalette);
                CPU_Regs.reg_eax.dword = Error.S_OK;
            }
        }
    };

    // HRESULT Unlock(this, LPVOID lpSurfaceData)
    static private Callback.Handler Unlock = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.Unlock";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpSurfaceData = CPU.CPU_Pop32();
            unlock(This);
            CPU_Regs.reg_eax.dword = Error.S_OK;
        }
    };

    // HRESULT UpdateOverlay(this, LPRECT lpSrcRect, LPDIRECTDRAWSURFACE lpDDDestSurface, LPRECT lpDestRect, DWORD dwFlags, LPDDOVERLAYFX lpDDOverlayFx)
    static private Callback.Handler UpdateOverlay = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.UpdateOverlay";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int lpSrcRect = CPU.CPU_Pop32();
            int lpDDDestSurface = CPU.CPU_Pop32();
            int lpDestRect = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDOverlayFx = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT UpdateOverlayDisplay(this, DWORD dwFlags)
    static private Callback.Handler UpdateOverlayDisplay = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.UpdateOverlayDisplay";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            notImplemented();
        }
    };

    // HRESULT UpdateOverlayZOrder(this, DWORD dwFlags, LPDIRECTDRAWSURFACE lpDDSReference)
    static private Callback.Handler UpdateOverlayZOrder = new HandlerBase() {
        public java.lang.String getName() {
            return "IDirectDrawSurface.UpdateOverlayZOrder";
        }
        public void onCall() {
            int This = CPU.CPU_Pop32();
            int dwFlags = CPU.CPU_Pop32();
            int lpDDSReference = CPU.CPU_Pop32();
            notImplemented();
        }
    };
}