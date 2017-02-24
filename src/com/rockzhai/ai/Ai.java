package com.rockzhai.ai;
/**
 * AI算法和主函数入口
 */
import java.util.*;  
import java.awt.*;  
import java.awt.event.*;  
import javax.swing.*;  

public class Ai{  
	//
    private static DrawingPanel panel=new DrawingPanel(700,700);  
    private static Graphics g=panel.getGraphics();  
    public static boolean isBlack=false;//标志棋子的颜色  
    public static int[][] chessBoard=new int[17][17]; //棋盘棋子的摆放情况：0无子，1黑子，－1白子  
    private static HashSet<Point> toJudge=new HashSet<Point>(); // ai可能会下棋的点  
    private static int dr[]=new int[]{-1,1,-1,1,0,0,-1,1}; // 方向向量  
    private static int dc[]=new int[]{1,-1,-1,1,-1,1,0,0}; //方向向量  
    public static final int MAXN=1<<28;  
    public static final int MINN=-MAXN;    
    private static int searchDeep=4;    //搜索深度  
    private static final int size=15;   //棋盘大小  
    public static boolean isFinished=false;  
  
    public static void main(String[] args){  
        MyMouseEvent myMouseEvent=new MyMouseEvent();  
        panel.addMouseListener(myMouseEvent);  
        initChessBoard();  
    }  
  
    // 初始化函数，绘图  
    public static void initChessBoard(){  
  
        isBlack=false;  
        toJudge.clear();  
        panel.clear();  
        panel.setBackground(Color.GRAY);  
        g.setColor(Color.BLACK);  
        for(int i=45;i<=675;i+=45){  
            g.drawLine(45,i,675,i);  
            g.drawLine(i,45,i,675);  
        }  
        // 棋盘上的五个定位基本点，图中的小圆圈  
        g.setColor(Color.BLACK);  
        g.fillOval(353,353,14,14);  
        g.fillOval(218,218,14,14);  
        g.fillOval(488,218,14,14);  
        g.fillOval(488,488,14,14);  
        g.fillOval(218,488,14,14);  
        // 初始化棋盘  
        for(int i=1;i<=15;++i)  
            for(int j=1;j<=15;++j)  
                chessBoard[i][j]=0;  
        // ai先手  
        g.fillOval(337,337,45,45);  
        chessBoard[8][8]=1;  
        for(int i=0;i<8;++i)  
            if(1<=8+dc[i] && 8+dc[i]<=size && 1<=8+dr[i] && 8+dr[i]<=size){  
                Point now=new Point(8+dc[i],8+dr[i]);  
                if(!toJudge.contains(now))  
                    toJudge.add(now);  
            }  
        isBlack=false;  
    }  
  
    // 通过点击事件，得到棋子位置进行下棋  
    public static void putChess(int x,int y){  
        if(isBlack)  
            g.setColor(Color.BLACK);  
        else   
            g.setColor(Color.WHITE);  
        g.fillOval(x-22,y-22,45,45);  
        chessBoard[y/45][x/45]=isBlack?1:-1;  
        if(isEnd(x/45,y/45)){  
            String s=Ai.isBlack?"黑子胜":"白子胜";  
            JOptionPane.showMessageDialog(null,s);  
            isBlack=true;  
            initChessBoard();  
        }  
        else{  
            Point p=new Point(x/45,y/45);  
            if(toJudge.contains(p))  
                toJudge.remove(p);  
            for(int i=0;i<8;++i){  
                Point now=new Point(p.x+dc[i],p.y+dr[i]);  
                if(1<=now.x && now.x<=size && 1<=now.y && now.y<=size && chessBoard[now.y][now.x]==0)  
                    toJudge.add(now);  
            }  
        }  
    }  
  
    // ai博弈入口函数  
    public static void myAI(){  
        Node node=new Node();  
        dfs(0,node,MINN,MAXN,null);  
        Point now=node.bestChild.p;  
        // toJudge.remove(now);  
        putChess(now.x*45,now.y*45);  
        isBlack=false;  
    }  
  
    // alpha beta dfs  
    private static void dfs(int deep,Node root,int alpha,int beta,Point p){  
        if(deep==searchDeep){  
            root.mark=getMark();  
            // System.out.printf("%d\t%d\t%d\n",p.x,p.y,root.mark);  
            return;  
        }  
        ArrayList<Point> judgeSet=new ArrayList<Point>();  
        Iterator it=toJudge.iterator();  
        while(it.hasNext()){  
            Point now=new Point((Point)it.next());  
            judgeSet.add(now);  
        }  
        it=judgeSet.iterator();  
        while(it.hasNext()){  
            Point now=new Point((Point)it.next());  
            Node node=new Node();  
            node.setPoint(now);  
            root.addChild(node);  
            boolean flag=toJudge.contains(now);  
            chessBoard[now.y][now.x]=((deep&1)==1)?-1:1;  
            if(isEnd(now.x,now.y)){  
                root.bestChild=node;  
                root.mark=MAXN*chessBoard[now.y][now.x];  
                chessBoard[now.y][now.x]=0;  
                return;  
            }  
  
            boolean flags[]=new boolean[8]; //标记回溯时要不要删掉  
            Arrays.fill(flags,true);  
            for(int i=0;i<8;++i){  
                Point next=new Point(now.x+dc[i],now.y+dr[i]);  
                if(1<=now.x+dc[i] && now.x+dc[i]<=size && 1<=now.y+dr[i] && now.y+dr[i]<=size && chessBoard[next.y][next.x]==0){  
                    if(!toJudge.contains(next)){  
                        toJudge.add(next);  
                    }  
                    else flags[i]=false;  
                }  
            }  
              
            if(flag)   
                toJudge.remove(now);  
            dfs(deep+1,root.getLastChild(),alpha,beta,now);  
            chessBoard[now.y][now.x]=0;  
            if(flag)  
                toJudge.add(now);  
            for(int i=0;i<8;++i)  
                if(flags[i])  
                    toJudge.remove(new Point(now.x+dc[i],now.y+dr[i]));  
            // alpha beta剪枝  
            // min层  
            if((deep&1)==1){  
                if(root.bestChild==null || root.getLastChild().mark<root.bestChild.mark){  
                    root.bestChild=root.getLastChild();  
                    root.mark=root.bestChild.mark;  
                    if(root.mark<=MINN)  
                        root.mark+=deep;  
                    beta=Math.min(root.mark,beta);  
                }  
                if(root.mark<=alpha)  
                    return;  
            }  
            // max层  
            else{  
                if(root.bestChild==null || root.getLastChild().mark>root.bestChild.mark){  
                    root.bestChild=root.getLastChild();  
                    root.mark=root.bestChild.mark;  
                    if(root.mark==MAXN)  
                        root.mark-=deep;  
                    alpha=Math.max(root.mark,alpha);  
                }  
                if(root.mark>=beta)  
                    return;  
            }  
        }  
        // if(deep==0) System.out.printf("******************************************\n");  
    }  
  
    public static int getMark(){  
        int res=0;  
        for(int i=1;i<=size;++i){  
            for(int j=1;j<=size;++j){  
                if(chessBoard[i][j]!=0){  
                    // 行  
                    boolean flag1=false,flag2=false;  
                    int x=j,y=i;  
                    int cnt=1;  
                    int col=x,row=y;  
                    while(--col>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(col>0 && chessBoard[row][col]==0) flag1=true;  
                    col=x;row=y;  
                    while(++col<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(col<=size && chessBoard[row][col]==0) flag2=true;  
                    if(flag1 && flag2)  
                        res+=chessBoard[i][j]*cnt*cnt;  
                    else if(flag1 || flag2) res+=chessBoard[i][j]*cnt*cnt/4;   
                    if(cnt>=5) res=MAXN*chessBoard[i][j];  
                    // 列  
                    col=x;row=y;  
                    cnt=1;flag1=false;flag2=false;  
                    while(--row>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(row>0 && chessBoard[row][col]==0) flag1=true;  
                    col=x;row=y;  
                    while(++row<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(row<=size && chessBoard[row][col]==0) flag2=true;  
                    if(flag1 && flag2)  
                        res+=chessBoard[i][j]*cnt*cnt;  
                    else if(flag1 || flag2)  
                        res+=chessBoard[i][j]*cnt*cnt/4;  
                    if(cnt>=5) res=MAXN*chessBoard[i][j];  
                    // 左对角线  
                    col=x;row=y;  
                    cnt=1;flag1=false;flag2=false;  
                    while(--col>0 && --row>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(col>0 && row>0 && chessBoard[row][col]==0) flag1=true;  
                    col=x;row=y;  
                    while(++col<=size && ++row<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(col<=size && row<=size && chessBoard[row][col]==0) flag2=true;  
                    if(flag1 && flag2)    
                        res+=chessBoard[i][j]*cnt*cnt;  
                    else if(flag1 || flag2) res+=chessBoard[i][j]*cnt*cnt/4;  
                    if(cnt>=5) res=MAXN*chessBoard[i][j];  
                    // 右对角线  
                    col=x;row=y;  
                    cnt=1;flag1=false;flag2=false;  
                    while(++row<=size && --col>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(row<=size && col>0 && chessBoard[row][col]==0) flag1=true;  
                    col=x;row=y;  
                    while(--row>0 && ++col<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
                    if(row>0 && col<=size && chessBoard[i][j]==0) flag2=true;  
                    if(flag1 && flag2)  
                        res+=chessBoard[i][j]*cnt*cnt;  
                    else if(flag1 || flag2) res+=chessBoard[i][j]*cnt*cnt/4;  
                    if(cnt>=5) res=MAXN*chessBoard[i][j];  
                      
                }  
            }  
        }  
        return res;  
    }  
  
    // for debug  
    public static void debug(){  
        for(int i=1;i<=size;++i){  
            for(int j=1;j<=size;++j){  
                System.out.printf("%d\t",chessBoard[i][j]);  
            }  
            System.out.println("");  
        }  
    }  
  
    // 判断是否一方取胜  
    public static boolean isEnd(int x,int y){  
        // 判断一行是否五子连珠  
        int cnt=1;  
        int col=x,row=y;  
        while(--col>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        col=x;row=y;  
        while(++col<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        if(cnt>=5){  
            isFinished=true;  
            return true;  
        }  
        // 判断一列是否五子连珠  
        col=x;row=y;  
        cnt=1;  
        while(--row>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        col=x;row=y;  
        while(++row<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        if(cnt>=5){  
            isFinished=true;  
            return true;  
        }  
        // 判断左对角线是否五子连珠  
        col=x;row=y;  
        cnt=1;  
        while(--col>0 && --row>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        col=x;row=y;  
        while(++col<=size && ++row<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        if(cnt>=5){  
            isFinished=true;  
            return true;  
        }  
        // 判断右对角线是否五子连珠  
        col=x;row=y;  
        cnt=1;  
        while(++row<=size && --col>0 && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        col=x;row=y;  
        while(--row>0 && ++col<=size && chessBoard[row][col]==chessBoard[y][x]) ++cnt;  
        if(cnt>=5){  
            isFinished=true;  
            return true;  
        }  
        return false;  
    }  
}  
  
  
  
// 树节点  
class Node{  
    public Node bestChild=null;  
    public ArrayList<Node> child=new ArrayList<Node>();  
    public Point p=new Point();  
    public int mark;  
    Node(){  
        this.child.clear();  
        bestChild=null;  
        mark=0;  
    }  
    public void setPoint(Point r){  
        p.x=r.x;  
        p.y=r.y;  
    }  
    public void addChild(Node r){  
        this.child.add(r);  
    }  
    public Node getLastChild(){  
        return child.get(child.size()-1);  
    }  
}  
  
// 实现鼠标事件接口  
class MyMouseEvent implements MouseListener{  
    public void mouseClicked(MouseEvent e){  
        int x=round(e.getX()),y=round(e.getY());  
        if(x>=45 && x<=675 && y>=45 && y<=675 && Ai.chessBoard[y/45][x/45]==0 && Ai.isBlack==false){  
            Ai.putChess(x,y);  
            if(!Ai.isFinished){  
                Ai.isBlack=true;  
                Ai.myAI();  
            }  
            Ai.isFinished=false;  
        }  
    }  
    // 得到鼠标点击点附近的棋盘精准点  
    public static int round(int x){  
        return (x%45<22)?x/45*45:x/45*45+45;  
    }  
    public void mouseExited(MouseEvent e){}  
    public void mouseEntered(MouseEvent e){}  
    public void mouseReleased(MouseEvent e){}  
    public void mousePressed(MouseEvent e){}  
}